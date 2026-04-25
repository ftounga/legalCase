package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-DT-30-01 : réponse de l'endpoint {@code /protection-rp-analysis}.
 */
public record ProtectionRpResponse(
        UUID caseFileId,
        boolean salarieEncoreProtege,
        int scoreConformite,
        ProtectionRpCalculator.VerdictLegalite verdictLegalite,
        List<ProtectionRpCalculator.CritereRegularite> criteresRemplis,
        List<ProtectionRpCalculator.CritereRegularite> criteresManquants,
        double indemniteForfaitaireMinEur,
        double salaireEvictionPotentielEur,
        int delaiContestationJours,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
