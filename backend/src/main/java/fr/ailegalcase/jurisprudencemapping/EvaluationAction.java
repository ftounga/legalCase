package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-02 — action proposée par Claude pour un mapping confronté à
 * un ou plusieurs arrêts entrants.
 */
public enum EvaluationAction {

    /** L'arrêt mappé reste pertinent, la jurisprudence n'a pas évolué. */
    CONFIRM,

    /** L'arrêt entrant enrichit le mapping en complément. */
    ADD,

    /** L'arrêt entrant remplace l'arrêt actuellement cité (revirement détecté). */
    REPLACE,

    /** L'arrêt actuellement cité est explicitement censuré ou n'a plus de sens. */
    ARCHIVE,

    /** Aucune action pertinente (les arrêts entrants ne concernent pas ce mapping). */
    NONE
}
