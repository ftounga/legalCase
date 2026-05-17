package fr.ailegalcase.casefile;

/**
 * SF-217-01 : qualification d'un bien sous régime de communauté légale belge.
 */
public enum QualificationBienBe {

    /** Bien commun (acquêt) — entre dans le patrimoine commun. */
    COMMUN,

    /** Bien propre — reste dans le patrimoine personnel d'un époux. */
    PROPRE,

    /** Bien propre financé en partie par des fonds communs (ou inversement) — ouvre droit à récompense. */
    MIXTE_RECOMPENSE,

    /** Critères contradictoires — qualification impossible à trancher. */
    INDETERMINE
}
