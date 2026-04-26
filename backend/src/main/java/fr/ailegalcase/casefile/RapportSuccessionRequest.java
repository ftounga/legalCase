package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * SF-FA-24-13 : requête d'analyse de rapport à succession (FR — art. 843-863
 * + 919 Cciv). Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.
 */
public record RapportSuccessionRequest(
        BigDecimal donationsRecuesEur,
        LocalDate dateDonation,
        BigDecimal valeurAuJourPartage,
        Boolean donationDispenseDeRapport,
        Boolean naturePresumeeNonRapportable,
        RapportSuccessionCalculator.QualiteHeritier qualiteHeritier
) {}
