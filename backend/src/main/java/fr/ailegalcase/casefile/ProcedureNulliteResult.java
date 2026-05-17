package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-DT-36-01 : résultat structuré de l'analyse des vices de procédure de
 * licenciement (FR).
 */
public record ProcedureNulliteResult(
        List<ProcedureNulliteLicenciementCalculator.ViceDetecte> vicesDetectes,
        int scoreNullite,
        ProcedureNulliteLicenciementCalculator.Verdict verdict,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
