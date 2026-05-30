package fr.ailegalcase.casefile;

/**
 * SF-218-15 : validité de la clause invoquée (cession / conscience) par le
 * journaliste professionnel (art. L.7112-5 CT). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <ul>
 *   <li>VALIDE : le fait générateur de la clause est constaté (cession ou
 *       cessation de publication pour la clause de cession ; changement notable
 *       de l'orientation pour la clause de conscience). La rupture est alors
 *       assimilée à un licenciement ouvrant droit à indemnité.</li>
 *   <li>NON_VALIDE : la clause est invoquée mais son fait générateur n'est pas
 *       constaté — voir le motif associé.</li>
 *   <li>SANS_OBJET : aucune clause n'est invoquée (licenciement, démission,
 *       faute grave).</li>
 * </ul>
 */
public enum JournalisteStatutClauseValidite {
    VALIDE,
    NON_VALIDE,
    SANS_OBJET
}
