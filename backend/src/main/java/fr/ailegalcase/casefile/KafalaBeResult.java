package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-03 : résultat structuré du moteur décisionnel BE de qualification du
 * recueil légal (kafala).
 *
 * <p>{@code motifsConditions} liste les motifs d'exclusion de l'effet adoptif
 * (verdict EFFET_ADOPTIF_EXCLU) ou les conditions à satisfaire (verdict
 * RECONNAISSANCE_SOUS_CONDITIONS). {@code actesAProduire} et {@code messages} ne
 * sont jamais vides.</p>
 */
public record KafalaBeResult(
        KafalaBeCalculator.KafalaBeVerdict verdict,
        List<String> motifsConditions,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages
) {}
