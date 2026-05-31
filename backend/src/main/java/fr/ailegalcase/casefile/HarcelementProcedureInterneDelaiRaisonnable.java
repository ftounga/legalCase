package fr.ailegalcase.casefile;

/**
 * SF-218-27 : appréciation du délai de réaction de l'employeur entre la réception
 * du signalement et l'ouverture de l'enquête interne (F-DT-59). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>OUI : délai ≤ 15 jours (réaction prompte).</li>
 *   <li>LIMITE : délai 16–60 jours (à justifier).</li>
 *   <li>NON : délai &gt; 60 jours, ou signalement reçu sans enquête ouverte
 *       (réaction tardive / inexistante).</li>
 * </ul>
 */
public enum HarcelementProcedureInterneDelaiRaisonnable {
    OUI,
    LIMITE,
    NON
}
