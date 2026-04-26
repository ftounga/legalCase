package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-24-13 : résultat structuré de l'analyse de rapport à succession
 * (FR — art. 843-863 + 919 Cciv).
 */
public record RapportSuccessionResult(
        BigDecimal donationsRecuesEur,
        LocalDate dateDonation,
        BigDecimal valeurAuJourPartage,
        boolean donationDispenseDeRapport,
        boolean naturePresumeeNonRapportable,
        RapportSuccessionCalculator.QualiteHeritier qualiteHeritier,
        String country,
        RapportSuccessionCalculator.VerdictObligation verdictObligation,
        RapportSuccessionCalculator.ModeRapport modeRapportRecommande,
        BigDecimal montantRapportable,
        int delaiPrescriptionAns,
        int scoreEligibilite,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
