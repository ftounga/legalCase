package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-206-07 : résultat structuré de l'analyse d'une demande de résiliation
 * judiciaire du contrat de travail aux torts de l'employeur (FRANCE).
 */
public record ResiliationJudiciaireCphResult(
        List<ResiliationJudiciaireCphCalculator.ManquementRetenu> manquementsRetenus,
        int scoreSolidite,
        ResiliationJudiciaireCphCalculator.Verdict verdict,
        ResiliationJudiciaireCphCalculator.DateEffetProbable dateEffetProbable,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
