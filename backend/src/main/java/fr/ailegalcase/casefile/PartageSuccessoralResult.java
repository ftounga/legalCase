package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-24-09 : résultat structuré du calcul d'analyse de la modalité de
 * partage successoral (FR — art. 815-840 Cciv + 1364 CPC).
 */
public record PartageSuccessoralResult(
        PartageSuccessoralCalculator.ModePartage modePartageDemande,
        int nombreCoheritiers,
        boolean consentementsTous,
        boolean presenceImmeubles,
        boolean accordsValuation,
        boolean desaccordPersistant,
        LocalDate dateDeces,
        double valeurMasseEur,
        String country,
        PartageSuccessoralCalculator.VerdictRecevabilite verdictRecevabilite,
        PartageSuccessoralCalculator.ModePartage modeRecommande,
        boolean basculeMode,
        int scoreEligibilite,
        int delaiInstructionMois,
        double fraisEstimesPct,
        double fraisEstimesEur,
        boolean risqueLicitation,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
