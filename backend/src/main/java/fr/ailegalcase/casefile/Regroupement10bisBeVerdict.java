package fr.ailegalcase.casefile;

/**
 * SF-215-05 : verdict d'éligibilité du dossier de regroupement familial 10bis.
 *
 * <ul>
 *   <li>ELIGIBLE : score ≥ 80 ET carte A en cours de validité —
 *       toutes conditions essentielles réunies.</li>
 *   <li>SOUS_RESERVE : score 50-79 ET carte A en cours de validité — au moins une
 *       condition manque mais le dossier reste défendable avec compléments / dérogation
 *       argumentée.</li>
 *   <li>INELIGIBLE : score &lt; 50 OU carte A expirée (règle dure — sans titre en cours
 *       de validité, le regroupement est juridiquement impossible peu importe le score).</li>
 * </ul>
 */
public enum Regroupement10bisBeVerdict {
    ELIGIBLE,
    SOUS_RESERVE,
    INELIGIBLE
}
