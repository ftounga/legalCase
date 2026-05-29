package fr.ailegalcase.casefile;

/**
 * SF-214-29 : type de refus opposé à la déclaration de nationalité française.
 *
 * <ul>
 *   <li>REFUS_ENREGISTREMENT — refus d'enregistrement de la déclaration
 *       (Cciv 26-3) : recours devant le TJ dans les 6 mois.</li>
 *   <li>CONTESTATION_NATIONALITE — contestation de la nationalité par le
 *       ministère public ou refus assimilé porté devant le TJ.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b>. Le type de refus n'affecte pas le calcul
 * du délai de 6 mois — il est conservé à titre informatif et de contexte.
 */
public enum NaturalisationRecoursTjTypeRefusEnum {
    REFUS_ENREGISTREMENT,
    CONTESTATION_NATIONALITE
}
