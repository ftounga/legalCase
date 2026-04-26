package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-24-07 : résultat structuré de l'analyse de réserve héréditaire et
 * action en réduction (FR — art. 913 + 914-1 + 920-928 Cciv).
 */
public record ReserveHereditaireResult(
        int nombreEnfants,
        boolean conjointSurvivant,
        BigDecimal montantSuccession,
        BigDecimal montantLibsTotal,
        LocalDate dateOuvertureSuccession,
        ReserveHereditaireCalculator.QualiteDemandeur qualiteDuDemandeur,
        String country,
        double quotiteDisponiblePct,
        double reservePct,
        BigDecimal montantReserveEur,
        BigDecimal montantQuotiteDispoEur,
        BigDecimal excedentReductibleEur,
        boolean actionReductionRecevable,
        long delaiPrescriptionRestantMois,
        ReserveHereditaireCalculator.VerdictRecevabilite verdictRecevabilite,
        int scoreEligibilite,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
