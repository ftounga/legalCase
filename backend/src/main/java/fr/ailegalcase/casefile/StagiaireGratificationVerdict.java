package fr.ailegalcase.casefile;

/**
 * SF-218-21 : verdict global de l'analyse du régime du stagiaire (gratification
 * minimale + risque de requalification en CDI, art. L.124-1 et s. du code de
 * l'éducation, F-DT-109). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>STAGE_CONFORME : le seuil de gratification n'est pas atteint, ou la
 *       gratification versée couvre le minimum dû, ET le risque de
 *       requalification est faible — le stage est conforme.</li>
 *   <li>RAPPEL_GRATIFICATION : la gratification minimale est due (présence
 *       supérieure à 2 mois / 44 jours) et un rappel reste exigible (versé
 *       inférieur au minimum), sans indice fort de requalification.</li>
 *   <li>REQUALIFICATION_PROBABLE : le risque de requalification du stage en
 *       contrat de travail (CDI) est élevé (poste de travail permanent,
 *       missions hors projet pédagogique, dépassement de la durée maximale de
 *       6 mois) — rappel de salaire sur la base du SMIC / minimum conventionnel
 *       et indemnités de rupture possibles.</li>
 * </ul>
 */
public enum StagiaireGratificationVerdict {
    STAGE_CONFORME,
    RAPPEL_GRATIFICATION,
    REQUALIFICATION_PROBABLE
}
