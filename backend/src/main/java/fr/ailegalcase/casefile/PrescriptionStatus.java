package fr.ailegalcase.casefile;

/**
 * F-285 / SF-285-01 — statut de prescription / forclusion de la qualification
 * d'entrée d'un dossier. Générique (transversal FR+BE, 3 domaines). F-285 ne
 * calcule pas la prescription au jour près (ce serait un outil décisionnel
 * dédié) : l'avocat pose le statut, avec une date limite optionnelle.
 */
public enum PrescriptionStatus {
    /** Délai de prescription encore à vérifier. */
    A_VERIFIER,
    /** Action dans les délais. */
    DANS_LES_DELAIS,
    /** Échéance proche — risque de forclusion. */
    RISQUE_FORCLUSION,
    /** Action prescrite / forclose. */
    PRESCRIT
}
