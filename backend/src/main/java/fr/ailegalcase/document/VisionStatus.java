package fr.ailegalcase.document;

/**
 * SF-148-03 : statut de l'enrichissement visuel d'une pièce.
 *
 * <ul>
 *   <li>{@link #NOT_APPLICABLE} — pièce non éligible à vision (signal 1 non
 *       déclenché, type blacklisté ou hors whitelist). Défaut.</li>
 *   <li>{@link #PENDING} — marquée pour enrichissement, appel Anthropic
 *       démarré ou en attente d'exécution.</li>
 *   <li>{@link #DONE} — description visuelle persistée avec succès.</li>
 *   <li>{@link #FAILED} — l'appel Anthropic a échoué, description absente
 *       (fail-open en amont).</li>
 * </ul>
 */
public enum VisionStatus {
    NOT_APPLICABLE,
    PENDING,
    DONE,
    FAILED
}
