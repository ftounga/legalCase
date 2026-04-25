package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.PacsDissolutionCalculator.CreanceType;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.ModeDissolution;
import fr.ailegalcase.casefile.PacsDissolutionCalculator.RegimeBiens;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-20-01 : résultat de l'analyse dissolution PACS
 * (art. 515-7 + 515-7-1 + 515-8 Cciv).
 * Outil single-country FRANCE + DROIT_FAMILLE.
 */
public record PacsDissolutionResult(
        LocalDate dateConclusionPacs,
        ModeDissolution modeDissolution,
        LocalDate dateDissolution,
        int dureeUnionAnnees,
        RegimeBiens regimeBiens,
        boolean patrimoineCommunSignificatif,
        List<CreanceType> creancesAlleguees,
        int enfantsCommuns,
        LocalDate dateNotificationPartenaire,
        boolean dissolutionValide,
        boolean delaiNotificationOk,
        boolean dureeUnionEligibleCreances,
        int scoreCreancesProbables,
        String verdictRecommandation,
        List<CreanceType> creancesPotentielleVisibles,
        int delaiPrescriptionAnnees,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
