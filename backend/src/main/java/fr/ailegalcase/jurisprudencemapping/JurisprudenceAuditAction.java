package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-01 — action tracée dans {@link JurisprudenceAuditLog}.
 *
 * <p>Préfixe {@code AUTO_} : action déclenchée par le cron full auto-pilot
 * (SF-JU-01-02). Préfixe {@code MANUAL_} : action déclenchée par un
 * SUPER_ADMIN via le dashboard (SF-JU-01-05).</p>
 */
public enum JurisprudenceAuditAction {

    /** Cron mensuel a confirmé qu'un mapping reste valide (last_verified_at mis à jour). */
    AUTO_CONFIRM,

    /** Cron a ajouté un arrêt en complément (confiance > 0.80). */
    AUTO_ADD,

    /** Cron a remplacé un arrêt par un autre (confiance > 0.85, revirement détecté). */
    AUTO_REPLACE,

    /** Cron a archivé un arrêt explicitement censuré ou un mapping orphelin. */
    AUTO_ARCHIVE,

    /** Action SUPER_ADMIN : remplacer l'arrêt cité par un arrêt entrant. */
    MANUAL_REPLACE,

    /** Action SUPER_ADMIN : ajouter un arrêt en complément. */
    MANUAL_ADD,

    /** Action SUPER_ADMIN : ignorer un flag (faux positif, signal d'apprentissage). */
    MANUAL_IGNORE
}
