package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-01 — origine d'un {@link JurisprudenceWatchFlag}.
 */
public enum JurisprudenceWatchFlagSource {

    /** Détecté par le cron mensuel SF-JU-01-02 (confiance Claude entre 0.60 et 0.85). */
    CRON,

    /** Signalé par un avocat utilisateur via le bouton « Signaler un problème » SF-JU-01-04. */
    USER_SIGNAL
}
