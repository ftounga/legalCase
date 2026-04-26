package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-13 : réponse de l'endpoint {@code /rapport-succession-analysis}.
 */
public record RapportSuccessionResponse(
        UUID caseFileId,
        BigDecimal donationsRecuesEur,
        LocalDate dateDonation,
        BigDecimal valeurAuJourPartage,
        boolean donationDispenseDeRapport,
        boolean naturePresumeeNonRapportable,
        RapportSuccessionCalculator.QualiteHeritier qualiteHeritier,
        RapportSuccessionCalculator.VerdictObligation verdictObligation,
        RapportSuccessionCalculator.ModeRapport modeRapportRecommande,
        BigDecimal montantRapportable,
        int delaiPrescriptionAns,
        int scoreEligibilite,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
