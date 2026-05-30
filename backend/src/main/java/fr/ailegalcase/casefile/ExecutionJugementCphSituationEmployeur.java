package fr.ailegalcase.casefile;

/**
 * SF-218-03 : situation de l'employeur débiteur au moment de l'exécution forcée
 * d'un jugement CPH. Détermine la voie d'exécution : recouvrement direct
 * (IN_BONIS) ou relais par la garantie AGS / saisine CGEA lorsque l'employeur
 * fait l'objet d'une procédure collective (REDRESSEMENT / LIQUIDATION,
 * L. 3253-6 et s. Code travail). Outil <b>FRANCE UNIQUEMENT</b>.
 */
public enum ExecutionJugementCphSituationEmployeur {
    IN_BONIS,
    REDRESSEMENT,
    LIQUIDATION
}
