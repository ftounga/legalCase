package fr.ailegalcase.casefile;

/**
 * SF-218-37 : verdict d'éligibilité à la monétisation de jours de RTT (rachat de
 * jours de RTT — loi n° 2022-1157 du 16/08/2022 art. 5, dispositif prolongé
 * jusqu'au 31/12/2026, F-DT-51). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>ELIGIBLE : les jours de RTT sont acquis dans la fenêtre du dispositif
 *       (01/01/2022 → 31/12/2026) → la monétisation est ouverte, montant brut
 *       majoré calculé.</li>
 *   <li>NON_ELIGIBLE : les jours sont hors de la fenêtre du dispositif → pas de
 *       monétisation, aucun montant calculé.</li>
 * </ul>
 */
public enum RttMonetisationStatut {
    ELIGIBLE,
    NON_ELIGIBLE
}
