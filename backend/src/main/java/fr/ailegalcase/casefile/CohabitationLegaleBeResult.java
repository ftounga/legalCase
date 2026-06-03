package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-01 : résultat structuré du moteur décisionnel BE de la cohabitation
 * légale.
 *
 * <p>{@code conditions} liste les conditions de formation manquantes (verdict
 * FORMATION_IMPOSSIBLE) ou les effets / conditions qualifiés (vues EFFETS /
 * DISSOLUTION). {@code actesAProduire} et {@code messages} ne sont jamais
 * vides.</p>
 */
public record CohabitationLegaleBeResult(
        CohabitationLegaleBeCalculator.CohabitationLegaleBeVerdict verdict,
        List<String> conditions,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages
) {}
