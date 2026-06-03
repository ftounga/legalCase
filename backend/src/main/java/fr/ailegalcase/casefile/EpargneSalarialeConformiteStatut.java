package fr.ailegalcase.casefile;

/**
 * SF-218-41 : verdict de conformité aux obligations d'épargne salariale
 * (intéressement / participation / dispositif de partage de la valeur — art.
 * L.3311-1 et s., L.3321-1 et s., L.3322-2 CT + loi n° 2023-1107 du 29/11/2023,
 * F-DT-53). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CONFORME : toutes les obligations applicables (participation obligatoire
 *       au-delà de 50 salariés, dispositif de partage de la valeur pour les
 *       entreprises de 11 à 49 salariés rentables) sont remplies.</li>
 *   <li>OBLIGATION_NON_REMPLIE : au moins une obligation applicable n'est pas
 *       satisfaite (participation obligatoire absente, ou aucun dispositif de
 *       partage de la valeur dans une entreprise rentable de 11 à 49
 *       salariés).</li>
 *   <li>NON_REQUIS : aucune obligation applicable (effectif inférieur à 11
 *       salariés, ou entreprise de 11 à 49 salariés non rentable).</li>
 * </ul>
 */
public enum EpargneSalarialeConformiteStatut {
    CONFORME,
    OBLIGATION_NON_REMPLIE,
    NON_REQUIS
}
