package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-223-02 : résultat structuré du moteur décisionnel BE de recevabilité de
 * l'adoption.
 *
 * <p>{@code motifsConditions} liste les motifs d'irrecevabilité (verdict
 * IRRECEVABLE) ou les conditions suspensives à satisfaire (verdict
 * RECEVABLE_SOUS_CONDITIONS). {@code actesAProduire} et {@code messages} ne sont
 * jamais vides.</p>
 */
public record AdoptionBeResult(
        AdoptionBeCalculator.AdoptionBeVerdict verdict,
        List<String> motifsConditions,
        List<String> actesAProduire,
        List<String> basesJuridiques,
        List<String> messages
) {}
