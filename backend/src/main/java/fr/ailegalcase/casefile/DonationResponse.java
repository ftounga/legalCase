package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-05 : réponse de l'endpoint {@code /donation-analysis}.
 */
public record DonationResponse(
        UUID caseFileId,
        DonationCalculator.FormeDonation formeDonation,
        DonationCalculator.VerdictValidite verdictValidite,
        List<DonationCalculator.RisqueIdentifie> risquesRequalification,
        boolean actionEnReductionPossible,
        boolean revocationPossible,
        int delaiContestationAns,
        int scoreEligibilite,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
