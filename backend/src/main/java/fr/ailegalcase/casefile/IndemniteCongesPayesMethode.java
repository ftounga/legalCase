package fr.ailegalcase.casefile;

/**
 * SF-DT-26-01 : méthodes de calcul de l'indemnité compensatrice de congés payés
 * (art. L.3141-24 Code du travail).
 *
 * <ul>
 *   <li>{@link #DIX_POURCENT} — 10 % de la rémunération totale brute de la période de
 *       référence (art. L.3141-24 I).</li>
 *   <li>{@link #MAINTIEN} — rémunération qui aurait été perçue si le salarié avait
 *       continué de travailler (méthode du trentième : salaire mensuel ÷ 30 × jours dus).</li>
 * </ul>
 *
 * Si l'avocat ne force pas une méthode, la méthode retenue est celle qui donne le
 * résultat le plus favorable au salarié (jurisprudence constante, art. L.3141-28).
 */
public enum IndemniteCongesPayesMethode {
    DIX_POURCENT,
    MAINTIEN
}
