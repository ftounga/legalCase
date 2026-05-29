package fr.ailegalcase.casefile;

/**
 * SF-214-27 : statut de la procédure d'évaluation d'âge MNA
 * (F-IM-38-mna-evaluation-age-fr, FRANCE uniquement).
 */
public enum MnaEvaluationAgeStatut {

    /** Évaluation en cours / pas encore refusée par l'ASE. */
    EN_ATTENTE_EVALUATION,

    /** Refus ASE → saisine du juge des enfants urgente (délai 5 j). */
    RECOURS_JE_URGENT,

    /** Examen osseux ordonné → contestation à articuler. */
    EXAMEN_OSSEUX_CONTESTE,

    /** Minorité reconnue / pris en charge par l'ASE. */
    PRIS_EN_CHARGE
}
