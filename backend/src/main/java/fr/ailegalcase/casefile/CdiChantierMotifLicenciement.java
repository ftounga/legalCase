package fr.ailegalcase.casefile;

/**
 * SF-218-25 : qualification du motif du licenciement pour fin de chantier
 * (art. L.1236-8 CT, F-DT-37). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>FIN_CHANTIER_CRS : la rupture pour fin de chantier repose sur une cause
 *       réelle et sérieuse (motif spécifique de licenciement, art. L.1236-8) —
 *       suppose un recours valide et un chantier effectivement achevé.</li>
 *   <li>MOTIF_NON_FONDE : le motif de fin de chantier n'est pas caractérisé
 *       (chantier non achevé, ou recours invalide) — le licenciement ne peut
 *       être fondé sur ce motif spécifique.</li>
 * </ul>
 */
public enum CdiChantierMotifLicenciement {
    FIN_CHANTIER_CRS,
    MOTIF_NON_FONDE
}
