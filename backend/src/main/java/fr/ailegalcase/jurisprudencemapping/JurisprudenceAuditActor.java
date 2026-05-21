package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-01 — acteur d'une action {@link JurisprudenceAuditAction}.
 */
public enum JurisprudenceAuditActor {

    /** Cron full auto-pilot Claude (SF-JU-01-02 ou SF-JU-01-03). */
    CRON,

    /** Action manuelle d'un SUPER_ADMIN via le dashboard SF-JU-01-05. */
    SUPER_ADMIN
}
