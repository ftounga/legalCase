package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-06 : résultat structuré du moteur décisionnel BE du régime de
 * séparation de biens.
 *
 * <p>{@code motifs} liste les motifs annotés de la qualification,
 * {@code effetsPatrimoniaux} décrit le sort des biens / la créance de
 * participation. {@code motifs} et {@code messages} ne sont jamais vides.</p>
 */
public record RegimeBeSeparationBiensResult(
        RegimeBeSeparationBiensCalculator.RegimeBeSeparationBiensVerdict verdict,
        List<String> motifs,
        String effetsPatrimoniaux,
        List<String> basesJuridiques,
        List<String> messages
) {}
