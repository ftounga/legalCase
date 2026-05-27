package fr.ailegalcase.casefile;

/**
 * SF-213-09 : ampleur de la modification unilatérale (Loi 03/07/1978 art. 20,
 * BELGIQUE) — pilote le verdict de l'analyseur acte équipollent à rupture.
 *
 * <p>La doctrine du Ius Variandi reconnaît à l'employeur un pouvoir limité
 * d'aménagement du contrat ; seules les modifications <b>substantielles</b>
 * portant sur un élément essentiel peuvent constituer un acte équipollent.
 * </p>
 */
public enum AmpleurModificationEnum {
    /** Modification mineure dans le périmètre du Ius Variandi. */
    MINEURE,
    /** Modification substantielle — point de bascule possible vers acte équipollent. */
    SUBSTANTIELLE,
    /** Ampleur non déterminable en l'état du dossier — analyse approfondie requise. */
    INDETERMINEES
}
