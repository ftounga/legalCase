package fr.ailegalcase.immigration;

import java.util.List;

/**
 * F-151 SF-151-01 : scénario stratégique immigration — une option juridique
 * ouverte à l'avocat sur un dossier (ex : changement de statut immédiat vs
 * attendre l'expiration, recours gracieux vs contentieux).
 *
 * <p>Produit par l'IA en liste de 2-3 scenarii comparatifs, exposé dans
 * {@code CaseAnalysisResponse.immigrationStrategyScenarios}.
 */
public record ImmigrationStrategyScenario(
        String scenarioLabel,
        String scenarioDescription,
        String baseLegale,
        String targetTitleCode,
        String targetTitleLabel,
        String delayDaysEstimate,
        String riskLevel,
        String riskJustification,
        List<String> requiredAdditionalPieces,
        List<String> advantages,
        List<String> drawbacks
) {
    public ImmigrationStrategyScenario {
        requiredAdditionalPieces = requiredAdditionalPieces == null ? List.of() : List.copyOf(requiredAdditionalPieces);
        advantages = advantages == null ? List.of() : List.copyOf(advantages);
        drawbacks = drawbacks == null ? List.of() : List.copyOf(drawbacks);
    }
}
