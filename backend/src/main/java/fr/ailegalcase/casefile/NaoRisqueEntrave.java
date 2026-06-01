package fr.ailegalcase.casefile;

/**
 * SF-218-29 : niveau de risque d'entrave / de sanction lié au défaut de négociation
 * annuelle obligatoire (NAO, art. L.2242-8, L.2243-2 CT, F-DT-66). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ELEVE : un bloc obligatoire n'a pas été engagé alors qu'un délégué syndical
 *       est présent (délit d'entrave L.2243-2, pénalité égalité F/H jusqu'à 1 % de
 *       la masse salariale).</li>
 *   <li>MODERE : verdict NON_CONFORME hors absence d'engagement des blocs
 *       (périodicité, PV de désaccord ou échéance dépassée).</li>
 *   <li>FAIBLE : verdict CONFORME ou NON_APPLICABLE (pas de DS désigné).</li>
 * </ul>
 */
public enum NaoRisqueEntrave {
    ELEVE,
    MODERE,
    FAIBLE
}
