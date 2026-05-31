package fr.ailegalcase.casefile;

/**
 * SF-218-25 : verdict global de l'analyse du licenciement pour fin de chantier
 * du CDI de chantier / d'opération (art. L.1223-8 et s. ; L.1236-8 CT,
 * F-DT-37). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>LICENCIEMENT_FONDE : recours valide + chantier achevé → le licenciement
 *       pour fin de chantier repose sur une cause réelle et sérieuse.</li>
 *   <li>LICENCIEMENT_A_SECURISER : recours valide mais motif fragile (chantier
 *       non achevé, ou poursuite sur un autre chantier non tracée) — risque à
 *       sécuriser avant notification.</li>
 *   <li>RECOURS_INVALIDE : aucun fondement valable au recours au CDI de chantier
 *       → requalification probable en CDI de droit commun, le motif de fin de
 *       chantier est inopérant.</li>
 * </ul>
 */
public enum CdiChantierVerdict {
    LICENCIEMENT_FONDE,
    LICENCIEMENT_A_SECURISER,
    RECOURS_INVALIDE
}
