package fr.ailegalcase.casefile;

/**
 * SF-213-01 : verdict de validité de la clause de non-concurrence BE.
 */
public enum ClauseNonConcurrenceBeVerdict {
    /** Clause valide — conditions de fond et de forme respectées. */
    VALIDE,
    /** Clause nulle de plein droit — au moins une condition substantielle est manquée. */
    NULLE,
    /** Clause partiellement nulle — zone géographique non justifiée notamment. */
    PARTIELLEMENT_NULLE
}
