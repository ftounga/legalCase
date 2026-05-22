package fr.ailegalcase.casefile;

/**
 * SF-216-15 : forme d'adoption demandée ou possible en intra-familial
 * (adoption de l'enfant du conjoint, art. 345-1 + 360-362 Cciv).
 *
 * <ul>
 *   <li>{@link #PLENIERE} — adoption plénière (art. 345-1 al. 2). Rompt
 *       toute filiation antérieure (irréversible, art. 356 Cciv). N'est
 *       possible que si l'autre parent biologique est décédé, déchu, ou y
 *       consent.</li>
 *   <li>{@link #SIMPLE} — adoption simple (art. 360-362). Cumulable avec la
 *       filiation d'origine — recommandée si l'autre parent biologique est
 *       vivant et non consentant.</li>
 * </ul>
 */
public enum FormeAdoptionEnum {
    PLENIERE,
    SIMPLE
}
