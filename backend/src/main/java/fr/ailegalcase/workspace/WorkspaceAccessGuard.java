package fr.ailegalcase.workspace;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * SF-156-01 : garde-fou factorisé pour les opérations d'écriture sur un
 * workspace.
 *
 * <p>Invariant SF-156-00 §1 : un workspace en {@code PENDING_PAYMENT} n'est
 * <strong>pas</strong> utilisable — toute action d'écriture (création de
 * dossier, invitation, upload, etc.) doit être refusée avec {@code 409}
 * tant que le webhook Stripe n'a pas activé le workspace.
 *
 * <p>Un workspace en {@code CANCELLED} est inaccessible — {@code 410 Gone}.
 *
 * <p>Cible d'application : <strong>tout service qui exécute une opération
 * d'écriture sur un workspace</strong>. Pour V1 SF-156-01, on applique sur
 * les deux endpoints représentatifs cités par la mini-spec :
 * {@code CaseFileService.create} et {@code WorkspaceInvitationService.createInvitation}.
 * Les autres endpoints d'écriture (upload document, mise à jour dossier, etc.)
 * sont protégés indirectement par leur dépendance sur ces deux points
 * d'entrée. Une extension à tous les endpoints peut faire l'objet d'une SF
 * dédiée (durcissement défense en profondeur).
 */
@Component
public class WorkspaceAccessGuard {

    /**
     * @throws ResponseStatusException 409 si {@code PENDING_PAYMENT},
     *                                 410 si {@code CANCELLED}.
     */
    public void requireActive(Workspace workspace) {
        if (workspace == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found");
        }
        String status = workspace.getStatus();
        if (WorkspaceStatus.PENDING_PAYMENT.equals(status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Workspace en attente d'activation — terminez le paiement pour utiliser ce workspace");
        }
        if (WorkspaceStatus.CANCELLED.equals(status)) {
            throw new ResponseStatusException(HttpStatus.GONE, "Workspace supprimé");
        }
    }
}
