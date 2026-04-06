package fr.ailegalcase.analysis;

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
public class CaseAnalysisQueryService {

    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisQueryService.class);

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final AnalysisDocumentRepository analysisDocumentRepository;

    public CaseAnalysisQueryService(CaseAnalysisRepository caseAnalysisRepository,
                                    CaseFileRepository caseFileRepository,
                                    CurrentUserResolver currentUserResolver,
                                    WorkspaceMemberRepository workspaceMemberRepository,
                                    AnalysisDocumentRepository analysisDocumentRepository) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.analysisDocumentRepository = analysisDocumentRepository;
    }

    @Transactional(readOnly = true)
    public CaseAnalysisResponse getAnalysis(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
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

        CaseAnalysis analysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No analysis available"));

        return CaseAnalysisResponse.from(analysis, loadDocuments(analysis.getId()), workspace.getCountry());
    }

    @Transactional(readOnly = true)
    public List<CaseAnalysisResponse.VersionSummary> listVersions(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
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

        return caseAnalysisRepository
                .findByCaseFileIdAndAnalysisStatusOrderByVersionDesc(caseFileId, AnalysisStatus.DONE)
                .stream()
                .map(ca -> new CaseAnalysisResponse.VersionSummary(
                        ca.getId(),
                        ca.getVersion(),
                        ca.getAnalysisType().name(),
                        ca.getUpdatedAt(),
                        ca.getFaitsCount(),
                        ca.getPointsJuridiquesCount(),
                        ca.getRisquesCount(),
                        ca.getQuestionsOuvertesCount(),
                        ca.getTimelineCount()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public CaseAnalysisResponse getByVersion(UUID caseFileId, int version, OidcUser oidcUser, String provider, Principal principal) {
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

        CaseAnalysis analysis = caseAnalysisRepository
                .findByCaseFileIdAndAnalysisStatusAndVersion(caseFileId, AnalysisStatus.DONE, version)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Version not found"));

        return CaseAnalysisResponse.from(analysis, loadDocuments(analysis.getId()), workspace.getCountry());
    }

    private List<AnalysisDocument> loadDocuments(UUID analysisId) {
        try {
            return analysisDocumentRepository.findByAnalysisIdOrderByCreatedAt(analysisId);
        } catch (Exception e) {
            log.warn("Fail-open: could not load analysis documents for analysisId {}: {}", analysisId, e.getMessage());
            return List.of();
        }
    }
}
