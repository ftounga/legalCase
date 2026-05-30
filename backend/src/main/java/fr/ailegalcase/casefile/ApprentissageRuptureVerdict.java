package fr.ailegalcase.casefile;

/**
 * SF-218-23 : verdict global de l'analyse de la rupture du contrat
 * d'apprentissage (art. L.6222-18 et s. CT, F-DT-110). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>RUPTURE_REGULIERE : la rupture est valide (rupture libre dans les
 *       45 jours, ou motif autorisé et formalités réunies au-delà).</li>
 *   <li>RUPTURE_IRREGULIERE : la rupture est irrégulière (sans motif au-delà
 *       des 45 jours) — risque de dommages-intérêts, saisine CPH.</li>
 *   <li>RUPTURE_A_SECURISER : motif recevable mais une formalité reste à
 *       accomplir (p. ex. procédure de licenciement pour faute grave).</li>
 * </ul>
 */
public enum ApprentissageRuptureVerdict {
    RUPTURE_REGULIERE,
    RUPTURE_IRREGULIERE,
    RUPTURE_A_SECURISER
}
