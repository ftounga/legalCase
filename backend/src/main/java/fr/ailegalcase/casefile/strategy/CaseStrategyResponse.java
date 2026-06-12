package fr.ailegalcase.casefile.strategy;

import java.time.Instant;

/**
 * F-286 / SF-286-01 — réponse de l'API stratégie de dossier.
 *
 * @param status          {@code READY} (une stratégie persistée existe),
 *                        {@code EMPTY} (jamais générée),
 *                        {@code EMPTY_INPUT} (génération demandée mais 0 outil calculé →
 *                        aucune stratégie inventée — reprise du principe F-258).
 * @param content         markdown de la reco, ou {@code null}.
 * @param generatedAt     date de génération, ou {@code null}.
 * @param toolsConsidered nombre de verdicts d'outils CALCULÉS pris en compte.
 * @param modelUsed       modèle Anthropic utilisé, ou {@code null}.
 */
public record CaseStrategyResponse(
        Status status,
        String content,
        Instant generatedAt,
        int toolsConsidered,
        String modelUsed
) {
    public enum Status { READY, EMPTY, EMPTY_INPUT }

    /** Aucune stratégie encore générée pour ce dossier. */
    public static CaseStrategyResponse empty() {
        return new CaseStrategyResponse(Status.EMPTY, null, null, 0, null);
    }

    /** Génération demandée alors qu'aucun outil n'est calculé : rien d'inventé. */
    public static CaseStrategyResponse emptyInput() {
        return new CaseStrategyResponse(Status.EMPTY_INPUT, null, null, 0, null);
    }

    /** Une stratégie persistée existe. */
    public static CaseStrategyResponse from(CaseStrategy strategy) {
        return new CaseStrategyResponse(
                Status.READY,
                strategy.getContent(),
                strategy.getGeneratedAt(),
                strategy.getToolsConsidered(),
                strategy.getModelUsed());
    }
}
