package fr.ailegalcase.casefile;

/**
 * SF-216-19 : motif de l'indignité successorale envisagée (art. 726-727 Cciv).
 *
 * <ul>
 *   <li>{@link #MEURTRE_TENTE} — tentative de meurtre du défunt → indignité
 *       de plein droit (art. 726 Cciv) si condamnation définitive.</li>
 *   <li>{@link #MEURTRE} — meurtre du défunt → indignité de plein droit
 *       (art. 726 Cciv) si condamnation définitive.</li>
 *   <li>{@link #TEMOIGNAGE_FAUX} — faux témoignage en justice contre le
 *       défunt → indignité judiciaire (art. 727 al. 2 Cciv).</li>
 *   <li>{@link #OBSTRUCTION_TESTAMENT} — destruction, recel ou obstruction
 *       au testament → indignité judiciaire (art. 727 al. 4 Cciv).</li>
 *   <li>{@link #NON_SIGNALEMENT_HOMICIDE} — non-signalement d'un homicide
 *       commis sur le défunt → indignité judiciaire (art. 727 al. 3 Cciv).</li>
 *   <li>{@link #VIOLENCES_GRAVES} — violences graves ayant entraîné la
 *       mort sans intention de la donner ou tortures et actes de barbarie
 *       → indignité judiciaire élargie (loi n°2022-1617 du 23/12/2022).</li>
 *   <li>{@link #AUTRE} — autre cause envisagée (à motiver dans la requête).</li>
 * </ul>
 */
public enum MotifIndigniteEnum {
    MEURTRE_TENTE,
    MEURTRE,
    TEMOIGNAGE_FAUX,
    OBSTRUCTION_TESTAMENT,
    NON_SIGNALEMENT_HOMICIDE,
    VIOLENCES_GRAVES,
    AUTRE
}
