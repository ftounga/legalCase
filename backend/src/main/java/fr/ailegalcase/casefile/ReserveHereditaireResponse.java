package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-FA-24-07 : réponse de l'endpoint {@code /reserve-heriditaire-analysis}.
 */
public record ReserveHereditaireResponse(
        UUID caseFileId,
        int nombreEnfants,
        boolean conjointSurvivant,
        BigDecimal montantSuccession,
        BigDecimal montantLibsTotal,
        LocalDate dateOuvertureSuccession,
        ReserveHereditaireCalculator.QualiteDemandeur qualiteDuDemandeur,
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
        List<String> messages,
        String country
) {}
