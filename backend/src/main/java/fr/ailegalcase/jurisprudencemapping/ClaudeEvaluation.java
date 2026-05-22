package fr.ailegalcase.jurisprudencemapping;

import java.math.BigDecimal;

/**
 * F-JU-01 / SF-JU-01-02 — décision Claude pour un mapping confronté à des
 * arrêts entrants.
 *
 * @param action          {@link EvaluationAction} à appliquer
 * @param arretChoisi     arrêt cible quand action {@code ADD} / {@code REPLACE} ({@code null} sinon)
 * @param confidenceScore confiance Claude entre 0.00 et 1.00
 * @param raison          courte justification structurée
 */
public record ClaudeEvaluation(
        EvaluationAction action,
        JudilibreArret arretChoisi,
        BigDecimal confidenceScore,
        String raison) {

    /** Fallback quand le parsing Claude échoue ou que le prompt renvoie une réponse invalide. */
    public static ClaudeEvaluation none(String raison) {
        return new ClaudeEvaluation(EvaluationAction.NONE, null, BigDecimal.ZERO, raison);
    }
}
