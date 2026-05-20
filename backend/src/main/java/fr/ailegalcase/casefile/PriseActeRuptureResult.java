package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-206-05 : résultat structuré de l'analyse d'une prise d'acte de la
 * rupture aux torts de l'employeur (FRANCE).
 */
public record PriseActeRuptureResult(
        List<PriseActeRuptureCalculator.GriefRetenu> griefsRetenus,
        int scoreSolidite,
        PriseActeRuptureCalculator.Verdict verdict,
        PriseActeRuptureCalculator.EffetProbable effetProbable,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
