package fr.ailegalcase.casefile;

/**
 * SF-214-35 : motif d'une mesure d'assignation à résidence (L. 731-1 CESEDA).
 * Le motif n'affecte pas le calcul de la durée maximale (135 jours) — il est
 * conservé à titre informatif et de contexte.
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français).
 */
public enum AssignationResidenceMotifEnum {
    /** Assignation aux fins d'exécution d'une OQTF. */
    EXECUTION_OQTF,
    /** Surveillance dans l'attente de l'exécution d'une mesure d'éloignement. */
    SURVEILLANCE_MESURE_ELOIGNEMENT,
    AUTRE
}
