package fr.ailegalcase.workspace;

/**
 * SF-156-01 : valeurs canoniques de {@code Workspace.status}.
 *
 * <p>La colonne {@code workspaces.status} est un {@code varchar(50)} libre,
 * mais ces constantes sont la source de vérité applicative et doivent être
 * utilisées partout au lieu de littéraux String.
 *
 * <ul>
 *   <li>{@code ACTIVE} : workspace pleinement utilisable.</li>
 *   <li>{@code PENDING_PAYMENT} : workspace nouvellement créé en attente de
 *       confirmation Stripe (webhook {@code customer.subscription.created}).
 *       Aucun endpoint d'écriture (dossier, invitation, upload) n'accepte un
 *       workspace dans cet état — invariant SF-156-00 §1.</li>
 *   <li>{@code CANCELLED} : workspace en cours de suppression ou supprimé
 *       suite à un paiement échoué / un timeout 24 h. Conservé pour audit
 *       avant cleanup définitif.</li>
 * </ul>
 *
 * Pas d'enum strict côté DB volontairement : ajouter une nouvelle valeur ne
 * nécessite aucune migration.
 */
public final class WorkspaceStatus {

    public static final String ACTIVE = "ACTIVE";
    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String CANCELLED = "CANCELLED";

    private WorkspaceStatus() {
        // utility
    }

    public static boolean isActive(Workspace workspace) {
        return workspace != null && ACTIVE.equals(workspace.getStatus());
    }
}
