package fr.ailegalcase.casefile;

/**
 * SF-218-09 : motif de discrimination invoqué à l'appui de l'action de groupe
 * (critères prohibés, art. L. 1132-1 Code travail). Outil <b>FRANCE
 * UNIQUEMENT</b>. La liste reprend les principaux critères de l'art. L. 1132-1 ;
 * {@code AUTRE} couvre tout autre critère prohibé.
 */
public enum ActionGroupeDiscriminationMotif {
    ORIGINE,
    SEXE,
    AGE,
    HANDICAP,
    ETAT_SANTE,
    GROSSESSE,
    ACTIVITE_SYNDICALE,
    RELIGION,
    ORIENTATION_SEXUELLE,
    AUTRE
}
