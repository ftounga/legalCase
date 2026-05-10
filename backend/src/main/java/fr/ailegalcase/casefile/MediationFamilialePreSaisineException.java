package fr.ailegalcase.casefile;

/**
 * SF-210-01 : exceptions à la tentative obligatoire de médiation familiale
 * pré-saisine JAF (Cciv art. 373-2-10 al. 3).
 */
public enum MediationFamilialePreSaisineException {

    /** Pas d'exception applicable — tentative obligatoire si motif in-scope. */
    AUCUNE,
    /** Urgence caractérisée. */
    URGENCE,
    /** Violences conjugales / familiales. */
    VIOLENCE,
    /** Accord conjoint préalable des parties (médiation déjà aboutie ou écartée d'un commun accord). */
    ACCORD_PREALABLE,
    /** Motif légitime (autre que urgence/violence/accord). */
    MOTIF_LEGITIME,
    /** Autre exception non typée. */
    AUTRE
}
