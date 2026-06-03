package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-222-04 : résultat de l'analyse assistance éducative — mineur en danger
 * (art. 375 et s. Cciv).
 */
public record AssistanceEducativeResult(
        VerdictAssistanceEducativeEnum verdict,
        String juridiction,
        String mesureOrientee,
        List<String> basesJuridiques,
        List<String> messages
) {}
