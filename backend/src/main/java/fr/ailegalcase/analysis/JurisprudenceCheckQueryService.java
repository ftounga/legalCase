package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-179 SF-179-01 — lecture des {@link JurisprudenceCheck} pour
 * {@code GET /api/v1/case-files/{caseFileId}/jurisprudence-checks}.
 *
 * <p>Isolation workspace stricte : un dossier hors du workspace de
 * l'utilisateur renvoie 404 (camouflage), pattern miroir
 * {@code PieceManquanteAlignmentService.getForLatestAnalysis}.</p>
 */
@Service
public class JurisprudenceCheckQueryService {

    private final JurisprudenceCheckRepository jurisprudenceCheckRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public JurisprudenceCheckQueryService(JurisprudenceCheckRepository jurisprudenceCheckRepository,
                                          CaseAnalysisRepository caseAnalysisRepository,
                                          CaseFileRepository caseFileRepository,
                                          WorkspaceMemberRepository workspaceMemberRepository,
                                          CurrentUserResolver currentUserResolver) {
        this.jurisprudenceCheckRepository = jurisprudenceCheckRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Renvoie les checks de la dernière analyse {@code DONE} du dossier.
     *
     * @return réponse — {@code checks} vide si aucune référence détectée
     * @throws ResponseStatusException 404 si le dossier n'existe pas ou est hors workspace
     */
    @Transactional(readOnly = true)
    public JurisprudenceCheckResponse getForCaseFile(UUID caseFileId,
                                                     OidcUser oidcUser,
                                                     Principal principal) {
        User user = currentUserResolver.resolve(
                oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        List<JurisprudenceCheck> checks = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .map(a -> jurisprudenceCheckRepository
                        .findByCaseAnalysisIdOrderByDocumentNameAscReferenceAsc(a.getId()))
                .orElseGet(List::of);

        return JurisprudenceCheckResponse.from(checks);
    }
}
