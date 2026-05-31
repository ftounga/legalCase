package fr.ailegalcase.casefile;

/**
 * SF-218-27 : verdict global de conformité de la procédure interne de traitement
 * d'un signalement de harcèlement côté employeur (art. L.1153-5-1, L.2314-1,
 * L.1152-4, L.4121-1 CT, F-DT-59). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONFORME : tous les items obligatoires de la checklist sont satisfaits.</li>
 *   <li>NON_CONFORME : au moins un item obligatoire non satisfait, hors carence
 *       d'enquête (référent CSE / employeur manquant, affichage non réalisé).</li>
 *   <li>CARENCE_GRAVE : un signalement a été reçu et l'enquête n'a pas été
 *       déclenchée / n'est pas contradictoire / le délai de réaction est NON →
 *       manquement à l'obligation de sécurité (L.4121-1) susceptible d'engager la
 *       responsabilité de l'employeur.</li>
 * </ul>
 */
public enum HarcelementProcedureInterneVerdict {
    CONFORME,
    NON_CONFORME,
    CARENCE_GRAVE
}
