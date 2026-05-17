package fr.ailegalcase.casefile;

/**
 * SF-217-01 : verdict global de composition du patrimoine sous régime de
 * communauté légale belge.
 */
public enum RegimeCommunauteLegaleBeVerdict {

    /** Aucun contrat de mariage : le régime légal s'applique de plein droit. */
    COMMUNAUTE_LEGALE_APPLICABLE,

    /** Un contrat de mariage est signé : la qualification légale est indicative. */
    REGIME_CONVENTIONNEL_DETECTE,

    /** Un ou plusieurs biens présentent des critères contradictoires/insuffisants. */
    QUALIFICATION_INCOMPLETE
}
