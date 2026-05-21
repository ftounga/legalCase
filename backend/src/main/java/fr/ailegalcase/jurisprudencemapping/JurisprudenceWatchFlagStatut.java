package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-01 — statut de traitement d'un {@link JurisprudenceWatchFlag}.
 */
public enum JurisprudenceWatchFlagStatut {

    /** En attente d'arbitrage SUPER_ADMIN via le dashboard SF-JU-01-05. */
    PENDING,

    /** Décision prise (REPLACE / ADD) et appliquée au mapping. */
    REVIEWED,

    /** Faux positif marqué — signal d'apprentissage pour le prompt Claude. */
    IGNORED
}
