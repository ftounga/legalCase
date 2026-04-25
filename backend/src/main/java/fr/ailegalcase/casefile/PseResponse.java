package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-DT-14-01 : réponse de l'endpoint {@code /pse-analysis}.
 */
public record PseResponse(
        UUID caseFileId,
        boolean pseRequis,
        int scoreConformite,
        PseCalculator.VerdictValidite verdictValidite,
        List<PseCalculator.CritereValidite> criteresRemplis,
        List<PseCalculator.CritereValidite> criteresManquants,
        int delaiContestationJours,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
