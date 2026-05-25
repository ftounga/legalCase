package fr.ailegalcase.casefile;

/**
 * SF-213-02 : statut de prescription d'une créance de rappel de salaire BE.
 *
 * <ul>
 *   <li>{@link #PRESCRIT} — délai dépassé, action impossible.</li>
 *   <li>{@link #IMMINENT} — délai ≤ 30 j, urgence d'action.</li>
 *   <li>{@link #NON_PRESCRIT} — délai &gt; 30 j.</li>
 * </ul>
 */
public enum RappelSalaireBeStatutPrescription {
    PRESCRIT,
    IMMINENT,
    NON_PRESCRIT
}
