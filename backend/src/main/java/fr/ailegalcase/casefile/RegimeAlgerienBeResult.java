package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-05 : résultat structuré du moteur décisionnel BE du corridor algérien.
 *
 * <p>{@code motifs} liste les motifs annotés de la qualification,
 * {@code effetsDot} décrit le sort de la dot (mahr) / effet patrimonial.
 * {@code motifs} et {@code messages} ne sont jamais vides.</p>
 */
public record RegimeAlgerienBeResult(
        RegimeAlgerienBeCalculator.RegimeAlgerienBeVerdict verdict,
        List<String> motifs,
        String effetsDot,
        List<String> basesJuridiques,
        List<String> messages
) {}
