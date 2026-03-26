package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

@Service
public class CaseFileStatsService {

    private final CaseFileRepository caseFileRepository;
    private final DocumentRepository documentRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final UsageEventRepository usageEventRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public CaseFileStatsService(CaseFileRepository caseFileRepository,
                                DocumentRepository documentRepository,
                                CaseAnalysisRepository caseAnalysisRepository,
                                UsageEventRepository usageEventRepository,
                                CurrentUserResolver currentUserResolver,
                                WorkspaceMemberRepository workspaceMemberRepository) {
        this.caseFileRepository = caseFileRepository;
        this.documentRepository = documentRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.usageEventRepository = usageEventRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional(readOnly = true)
    public CaseFileStatsResponse getStats(UUID caseFileId, OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        var workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));

        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        long documentCount = documentRepository.countByCaseFileId(caseFileId);
        long analysisCount = caseAnalysisRepository.countByCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE);
        long totalTokens = usageEventRepository.sumTokensByCaseFileId(caseFileId);

        return new CaseFileStatsResponse(documentCount, analysisCount, totalTokens);
    }
}
