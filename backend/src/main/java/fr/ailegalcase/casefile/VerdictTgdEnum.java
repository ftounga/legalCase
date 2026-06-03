package fr.ailegalcase.casefile;

/**
 * SF-222-02 : verdict 3 niveaux de l'outil TGD (téléphone grave danger —
 * éligibilité, art. 41-3-1 CPP). Famille FR uniquement.
 *
 * <p>L'outil <b>conseille</b> l'avocat sur l'éligibilité ; l'<b>attribution</b>
 * du TGD relève du procureur de la République.</p>
 */
public enum VerdictTgdEnum {
    /** Tous les critères d'éligibilité réunis — solliciter le TGD auprès du parquet. */
    ELIGIBLE_TGD,
    /**
     * Éligible sous réserve : l'interdiction de contact issue d'une procédure
     * n'est pas encore prononcée → la faire prononcer d'abord (ex. ordonnance de
     * protection F-FA-14) avant de solliciter le TGD.
     */
    ELIGIBLE_SOUS_RESERVE,
    /** Un critère dur (autre que l'interdiction de contact) manque — non éligible en l'état. */
    NON_ELIGIBLE
}
