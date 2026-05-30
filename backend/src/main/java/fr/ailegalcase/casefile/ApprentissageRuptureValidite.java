package fr.ailegalcase.casefile;

/**
 * SF-218-23 : validité du motif de rupture du contrat d'apprentissage
 * (art. L.6222-18 et s. CT, F-DT-110). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>VALIDE : la rupture est régulière (rupture libre dans les 45 jours, ou
 *       motif autorisé au-delà).</li>
 *   <li>NON_VALIDE : la rupture est irrégulière (rupture sans motif au-delà des
 *       45 jours) — dommages-intérêts possibles, saisine du conseil de
 *       prud'hommes.</li>
 *   <li>A_SECURISER : motif recevable mais une formalité manque (p. ex. faute
 *       grave sans procédure de licenciement).</li>
 * </ul>
 */
public enum ApprentissageRuptureValidite {
    VALIDE,
    NON_VALIDE,
    A_SECURISER
}
