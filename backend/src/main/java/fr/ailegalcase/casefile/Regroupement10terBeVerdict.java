package fr.ailegalcase.casefile;

/**
 * SF-215-03 : verdict d'éligibilité du dossier de regroupement familial 10ter.
 *
 * <ul>
 *   <li>ELIGIBLE : score ≥ 80 — toutes conditions essentielles réunies.</li>
 *   <li>SOUS_RESERVE : score 50-79 — au moins une condition manque mais le
 *       dossier reste défendable avec compléments / dérogation argumentée.</li>
 *   <li>INELIGIBLE : score &lt; 50 — refus quasi certain en l'état.</li>
 * </ul>
 */
public enum Regroupement10terBeVerdict {
    ELIGIBLE,
    SOUS_RESERVE,
    INELIGIBLE
}
