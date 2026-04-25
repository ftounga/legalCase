package fr.ailegalcase.casefile;

/**
 * SF-DT-25-01 : origine de la durée de préavis retenue par le calcul.
 * <ul>
 *   <li>{@link #LEGALE} : durée prévue par l'article L.1234-1 (≥ 6 mois → 1 mois ; ≥ 2 ans → 2 mois).</li>
 *   <li>{@link #CCN} : durée prévue par la convention collective, strictement plus favorable que la légale.</li>
 *   <li>{@link #USAGE} : aucune durée légale ni CCN connue (typiquement ancienneté &lt; 6 mois sans CCN) ;
 *       l'avocat doit vérifier l'usage local.</li>
 * </ul>
 */
public enum IndemnitePreavisSourceDuree {
    LEGALE,
    CCN,
    USAGE
}
