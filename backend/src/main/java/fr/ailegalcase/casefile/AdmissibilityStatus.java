package fr.ailegalcase.casefile;

/**
 * F-285 / SF-285-01 — verdict de recevabilité / compétence de la qualification
 * d'entrée d'un dossier. Générique (transversal FR+BE, 3 domaines).
 */
public enum AdmissibilityStatus {
    /** Recevabilité encore à vérifier (état par défaut d'une qualification amorcée). */
    A_VERIFIER,
    /** Action jugée recevable / juridiction compétente. */
    RECEVABLE,
    /** Action jugée irrecevable / juridiction incompétente. */
    IRRECEVABLE
}
