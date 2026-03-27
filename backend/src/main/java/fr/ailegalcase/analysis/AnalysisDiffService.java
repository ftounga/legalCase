package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public class AnalysisDiffService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisDiffService.class);

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final AnalysisDiffCacheRepository analysisDiffCacheRepository;
    private final SemanticDiffService semanticDiffService;
    private final AnalysisDocumentRepository analysisDocumentRepository;
    private final ObjectMapper objectMapper;

    public AnalysisDiffService(CaseAnalysisRepository caseAnalysisRepository,
                               CaseFileRepository caseFileRepository,
                               WorkspaceMemberRepository workspaceMemberRepository,
                               CurrentUserResolver currentUserResolver,
                               AnalysisDiffCacheRepository analysisDiffCacheRepository,
                               SemanticDiffService semanticDiffService,
                               AnalysisDocumentRepository analysisDocumentRepository,
                               ObjectMapper objectMapper) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.analysisDiffCacheRepository = analysisDiffCacheRepository;
        this.semanticDiffService = semanticDiffService;
        this.analysisDocumentRepository = analysisDocumentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisDiffResponse diff(UUID caseFileId, UUID fromId, UUID toId,
                                     OidcUser oidcUser, String provider, Principal principal) {
        if (fromId.equals(toId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "fromId and toId must be different");
        }

        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        CaseAnalysis from = caseAnalysisRepository.findByIdAndCaseFileId(fromId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));

        CaseAnalysis to = caseAnalysisRepository.findByIdAndCaseFileId(toId, caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));

        if (from.getAnalysisStatus() != AnalysisStatus.DONE || to.getAnalysisStatus() != AnalysisStatus.DONE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Both analyses must have status DONE");
        }

        // Cache hit
        try {
            var cached = analysisDiffCacheRepository.findByFromIdAndToId(fromId, toId);
            if (cached.isPresent()) {
                return objectMapper.readValue(cached.get().getResultJson(), AnalysisDiffResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to read diff cache for ({}, {}), recomputing: {}", fromId, toId, e.getMessage());
            analysisDiffCacheRepository.findByFromIdAndToId(fromId, toId)
                    .ifPresent(analysisDiffCacheRepository::delete);
        }

        // Compute
        List<AnalysisDocument> fromDocs = analysisDocumentRepository.findByAnalysisIdOrderByCreatedAt(fromId);
        List<AnalysisDocument> toDocs = analysisDocumentRepository.findByAnalysisIdOrderByCreatedAt(toId);

        AnalysisDiffResponse response = semanticDiffService.diff(from, to, fromDocs, toDocs);

        // Persist cache
        try {
            AnalysisDiffCache cache = new AnalysisDiffCache();
            cache.setFromId(fromId);
            cache.setToId(toId);
            cache.setResultJson(objectMapper.writeValueAsString(response));
            analysisDiffCacheRepository.save(cache);
        } catch (Exception e) {
            log.warn("Failed to persist diff cache for ({}, {}): {}", fromId, toId, e.getMessage());
        }

        return response;
    }
}
