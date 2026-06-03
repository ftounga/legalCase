package fr.ailegalcase.casefile;

/**
 * SF-218-49 : verdict de l'outil "RTT — acquisition selon accord d'aménagement"
 * (art. L.3121-41 à L.3121-44 CT, F-DT-80). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>CALCULE : un accord d'aménagement du temps de travail sur l'année est
 *       présent ; le nombre théorique de JRTT acquis est calculé (sans
 *       majoration).</li>
 *   <li>RENVOI_HEURES_SUP : aucun accord d'aménagement ; les heures effectuées
 *       au-delà de 35 h relèvent du régime des heures supplémentaires (renvoi à
 *       l'outil dédié F-DT-19).</li>
 * </ul>
 */
public enum RttAcquisitionStatut {
    CALCULE,
    RENVOI_HEURES_SUP
}
