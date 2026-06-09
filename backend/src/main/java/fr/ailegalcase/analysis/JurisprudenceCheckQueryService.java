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

    /** Statuts d'un check sur lesquels le marquage « adverse à réfuter » est autorisé. */
    private static final List<JurisprudenceCheckStatus> MARKABLE_STATUSES =
            List.of(JurisprudenceCheckStatus.SUSPECT, JurisprudenceCheckStatus.NOT_FOUND);

    /**
     * F-98 / SF-98-56 — marque (ou démarque) une citation comme « adverse à réfuter ».
     *
     * <p>Isolation workspace stricte (pattern miroir de {@link #getForCaseFile}) : un check
     * hors du dossier / du workspace de l'utilisateur renvoie 404 (camouflage d'existence).
     * Garde de statut : le marquage n'est autorisé que sur les statuts réfutables
     * ({@code SUSPECT} / {@code NOT_FOUND}) — sinon 422.</p>
     *
     * @param markedAdverse nouvelle valeur du marquage
     * @return le {@link JurisprudenceCheckResponse.Check} à jour
     * @throws ResponseStatusException 404 si le dossier/check est introuvable ou hors
     *                                 workspace ; 422 si le statut n'est pas réfutable
     */
    @Transactional
    public JurisprudenceCheckResponse.Check markAdverse(UUID caseFileId,
                                                        UUID checkId,
                                                        boolean markedAdverse,
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

        JurisprudenceCheck check = jurisprudenceCheckRepository.findById(checkId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Jurisprudence check not found"));
        // Isolation : le check doit appartenir au dossier (et donc au workspace) ciblé.
        if (check.getCaseFile() == null || !check.getCaseFile().getId().equals(caseFileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Jurisprudence check not found");
        }

        if (!MARKABLE_STATUSES.contains(check.getStatut())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Seules les citations suspectes ou introuvables peuvent être marquées comme adverses à réfuter.");
        }

        check.setMarkedAdverse(markedAdverse);
        jurisprudenceCheckRepository.save(check);
        return JurisprudenceCheckResponse.toCheck(check);
    }
}
