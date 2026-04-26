package fr.ailegalcase.casefile;

import java.util.List;

/**
 * SF-FA-16-01 : résultat structuré de l'analyse du régime conventionnel
 * de communauté universelle (FR — art. 1526 + 1527 al. 2 Cciv).
 */
public record CommunauteUniverselleResult(
        CommunauteUniverselleCalculator.DispositifAnalyse dispositifAnalyse,
        Boolean contratNotarie,
        Boolean inscriptionEtatCivil,
        Boolean consentementLibreDesEpoux,
        Boolean respectReserveHereditaire,
        Boolean clauseAttributionIntegrale,
        Boolean enfantsNonCommuns,
        Double valeurCommunauteEur,
        String country,
        CommunauteUniverselleCalculator.VerdictValidite verdictValidite,
        int scoreValidite,
        boolean actionRetranchementPossible,
        int partAttributionConjointPct,
        double valeurAttributionEur,
        List<String> risquesIdentifies,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
