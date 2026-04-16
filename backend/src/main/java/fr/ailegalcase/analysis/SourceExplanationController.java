package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * Endpoint transversal (SF-IA-03-15a) exposant les phrases d'explication
 * de source générées par Haiku pour le dernier CaseAnalysis DONE du dossier.
 * Consommé par tous les outils décisionnels (F-DT-07/08/09/10, F-FA-*, F-IM-*).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/source-explanations")
public class SourceExplanationController {

    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final SourceExplanationRepository sourceExplanationRepository;

    public SourceExplanationController(CaseFileRepository caseFileRepository,
                                       WorkspaceMemberRepository workspaceMemberRepository,
                                       CurrentUserResolver currentUserResolver,
                                       CaseAnalysisRepository caseAnalysisRepository,
                                       SourceExplanationRepository sourceExplanationRepository) {
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.sourceExplanationRepository = sourceExplanationRepository;
    }

    @GetMapping
    public List<SourceExplanationResponse> list(@PathVariable UUID caseFileId,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");

        return caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                        caseFileId, AnalysisStatus.DONE)
                .map(analysis -> sourceExplanationRepository.findByCaseAnalysisId(analysis.getId()).stream()
                        .map(SourceExplanationResponse::from)
                        .toList())
                .orElseGet(List::of);
    }
}
