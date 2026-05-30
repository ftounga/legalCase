package fr.ailegalcase.casefile;

/**
 * SF-218-01 : qualité du représentant constitué pour l'appel social. En appel
 * d'un jugement CPH, la représentation est <b>obligatoire</b> : avocat ou
 * défenseur syndical (R. 1461-2 CPC). {@code AUCUNE} déclenche un item bloquant
 * dans la checklist. Outil <b>FRANCE UNIQUEMENT</b>.
 */
public enum AppelCphRepresentation {
    AVOCAT,
    DEFENSEUR_SYNDICAL,
    AUCUNE
}
