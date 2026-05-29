package fr.ailegalcase.casefile;

/**
 * SF-214-41 : statut du recours contre un retrait de titre pour fraude
 * (art. L. 412-7 CESEDA) au regard du délai de saisine du tribunal administratif
 * (2 mois à compter de la notification du retrait).
 *
 * <ul>
 *   <li>RECOURS_POSSIBLE : délai non expiré, marge &gt; 15 jours.</li>
 *   <li>URGENT : délai expirant dans moins de 15 jours — saisine à anticiper.</li>
 *   <li>PRESCRIT : délai de recours TA dépassé.</li>
 * </ul>
 *
 * <p>Outil <b>FRANCE UNIQUEMENT</b> (droit des étrangers français).
 */
public enum RetraitTitreFraudeStatut {
    RECOURS_POSSIBLE,
    URGENT,
    PRESCRIT
}
