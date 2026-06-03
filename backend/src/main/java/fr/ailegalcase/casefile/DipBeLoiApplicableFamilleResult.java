package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-07 : résultat structuré du moteur décisionnel BE de détermination de la
 * loi applicable en droit de la famille (DIP).
 *
 * <p>{@code loiApplicableDeterminee} est le code pays ISO-2 (ou label) de la loi
 * désignée, {@code null} si {@code LOI_INDETERMINABLE}. {@code fondementRattachement}
 * trace le critère retenu. {@code motifs} et {@code messages} ne sont jamais
 * vides.</p>
 */
public record DipBeLoiApplicableFamilleResult(
        DipBeLoiApplicableFamilleCalculator.DipBeLoiApplicableFamilleVerdict verdict,
        String loiApplicableDeterminee,
        DipBeLoiApplicableFamilleCalculator.FondementRattachement fondementRattachement,
        List<String> motifs,
        List<String> conseils,
        List<String> basesJuridiques,
        List<String> messages
) {}
