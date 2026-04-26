package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-FA-24-09 : requête d'analyse de la modalité de partage successoral
 * (FR — art. 815-840 Cciv + 1364 CPC).
 *
 * <p>Le pays n'est pas transmis dans le body — il est dérivé de
 * {@code caseFile.getWorkspace().getCountry()} côté service.</p>
 */
public record PartageSuccessoralRequest(
        PartageSuccessoralCalculator.ModePartage modePartageDemande,
        Integer nombreCoheritiers,
        Boolean consentementsTous,
        Boolean presenceImmeubles,
        Boolean accordsValuation,
        Boolean desaccordPersistant,
        LocalDate dateDeces,
        Double valeurMasseEur
) {}
