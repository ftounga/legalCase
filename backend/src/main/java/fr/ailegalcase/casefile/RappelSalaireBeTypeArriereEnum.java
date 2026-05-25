package fr.ailegalcase.casefile;

/**
 * SF-213-02 : type d'arriéré de salaire BE — pilote le délai de prescription
 * applicable au sens de la Loi du 03/07/1978 art. 15.
 *
 * <ul>
 *   <li>{@link #PENDANT_CONTRAT} — arriérés exigibles pendant l'exécution du
 *       contrat. Prescription <b>5 ans</b> à compter de la date d'exigibilité
 *       (date de fin de période).</li>
 *   <li>{@link #POST_RUPTURE} — créances exigibles après la rupture.
 *       Prescription <b>1 an</b> à compter de la date de rupture.</li>
 *   <li>{@link #MIXTE} — créances chevauchant la rupture. La prescription la
 *       plus stricte (1 an post-rupture) prime en V1 — pour les cas mixtes,
 *       l'avocat doit splitter manuellement la créance et lancer deux
 *       analyses séparées si nécessaire.</li>
 * </ul>
 */
public enum RappelSalaireBeTypeArriereEnum {
    PENDANT_CONTRAT,
    POST_RUPTURE,
    MIXTE
}
