package fr.ailegalcase.casefile;

/**
 * SF-217-01 : qualification d'une dette sous régime de communauté légale belge.
 */
public enum QualificationDetteBe {

    /** Dette commune — engage le patrimoine commun. */
    DETTE_COMMUNE,

    /** Dette propre — engage le seul patrimoine propre de l'époux concerné. */
    DETTE_PROPRE,

    /** Dette propre mais le créancier peut poursuivre les biens communs (recours interne par récompense). */
    DETTE_PROPRE_AVEC_RECOURS
}
