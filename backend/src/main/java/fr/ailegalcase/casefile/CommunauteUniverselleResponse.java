package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-16-01 : réponse de l'endpoint {@code /communaute-universelle-analysis}.
 */
public record CommunauteUniverselleResponse(
        UUID caseFileId,
        CommunauteUniverselleCalculator.DispositifAnalyse dispositifAnalyse,
        CommunauteUniverselleCalculator.VerdictValidite verdictValidite,
        int scoreValidite,
        boolean actionRetranchementPossible,
        int partAttributionConjointPct,
        double valeurAttributionEur,
        List<String> risquesIdentifies,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
