package fr.ailegalcase.casefile;

/**
 * SF-218-03 : verdict d'orientation de l'exécution forcée d'un jugement CPH.
 * Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>EXECUTION_DIRECTE : employeur in bonis — recouvrement direct par voie
 *       d'exécution (signification, commandement, mesures conservatoires).
 *       L'exécution provisoire de droit des créances salariales s'applique
 *       (art. R. 1454-28 CPC ; art. 514 CPC).</li>
 *   <li>RELAIS_AGS : employeur en redressement / liquidation judiciaire — les
 *       créances salariales sont garanties par l'AGS dans la limite des plafonds
 *       (L. 3253-6 et s. Code travail). Déclaration de créance au mandataire et
 *       saisine du CGEA requises.</li>
 *   <li>BLOQUE_INFO_MANQUANTE : procédure collective déclarée sans date
 *       d'ouverture — information indispensable au calcul de la garantie AGS
 *       (période garantie, plafonds) manquante.</li>
 * </ul>
 */
public enum ExecutionJugementCphVerdict {
    EXECUTION_DIRECTE,
    RELAIS_AGS,
    BLOQUE_INFO_MANQUANTE
}
