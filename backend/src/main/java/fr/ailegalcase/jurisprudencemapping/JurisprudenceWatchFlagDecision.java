package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-01 — décision prise sur un {@link JurisprudenceWatchFlag} lors
 * de son arbitrage par le dashboard SUPER_ADMIN (SF-JU-01-05).
 */
public enum JurisprudenceWatchFlagDecision {

    /** L'arrêt entrant remplace l'arrêt actuellement cité (ancien archivé). */
    REPLACE,

    /** L'arrêt entrant est ajouté en complément (cité côte à côte). */
    ADD,

    /** Faux positif — flag ignoré, marqué pour amélioration du prompt Claude. */
    IGNORE
}
