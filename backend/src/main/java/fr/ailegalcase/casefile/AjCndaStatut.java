package fr.ailegalcase.casefile;

/**
 * SF-214-19 : statut de l'éligibilité / des délais d'aide juridictionnelle (AJ)
 * devant la Cour nationale du droit d'asile (CNDA).
 *
 * <ul>
 *   <li>AJ_A_DEMANDER : éligible aux ressources, dans le délai, demande non encore
 *       déposée — action à conduire.</li>
 *   <li>AJ_DEPOSEE : la demande d'AJ a déjà été déposée.</li>
 *   <li>HORS_DELAI_AJ : délai de demande d'AJ (15 j) dépassé sans dépôt.</li>
 *   <li>NON_ELIGIBLE_RESSOURCES : ressources mensuelles supérieures au plafond AJ.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit d'asile français, loi 91-647, L. 532-4
 * CESEDA).
 */
public enum AjCndaStatut {
    AJ_A_DEMANDER,
    AJ_DEPOSEE,
    HORS_DELAI_AJ,
    NON_ELIGIBLE_RESSOURCES
}
