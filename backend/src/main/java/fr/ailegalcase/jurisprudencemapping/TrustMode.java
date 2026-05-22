package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-02 — niveau d'automatisation du cron veille mensuelle.
 *
 * <p>Propriété {@code jurisprudence.watch.trust-mode}, défaut {@link #AUTO_PILOT}.</p>
 */
public enum TrustMode {

    /** Toutes les décisions passent en flag PENDING — revue humaine systématique. */
    PARANOIA,

    /** Auto-action si confidence ≥ 0.85, flag PENDING entre 0.60 et 0.85. */
    EQUILIBRE,

    /** Identique à EQUILIBRE en V1 (différenciation V2 par signal). */
    AUTO_PILOT
}
