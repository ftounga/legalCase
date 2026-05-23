package fr.ailegalcase.casefile;

/**
 * SF-216-13 : modalité de l'audition du mineur (art. 388-1 al. 3 Cciv +
 * art. 1074-2 CPC).
 *
 * <ul>
 *   <li>{@link #SEUL} — audition par le juge seul, hors présence des
 *       parties (modalité par défaut — protège la parole de l'enfant).</li>
 *   <li>{@link #AVEC_AVOCAT} — audition avec l'assistance d'un avocat
 *       désigné pour l'enfant (art. 388-1 al. 2 Cciv). Recommandé en
 *       contexte conflictuel.</li>
 *   <li>{@link #AVEC_TIERS} — audition avec un tiers de confiance
 *       (psychologue, travailleur social) — modalité dérogatoire.</li>
 * </ul>
 */
public enum ModaliteAuditionEnum {
    SEUL,
    AVEC_AVOCAT,
    AVEC_TIERS
}
