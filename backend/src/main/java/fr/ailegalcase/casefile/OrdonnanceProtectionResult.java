package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-FA-14-01 : résultat de l'analyse d'ordonnance de protection FR
 * (art. 515-9 à 515-13 Cciv + Loi 30/07/2020 BAR).
 */
public record OrdonnanceProtectionResult(
        LocalDate dateRequete,
        List<String> violencesAlleguees,
        List<String> preuvesViolences,
        boolean dangerImmediat,
        boolean presenceEnfants,
        List<Integer> ageEnfants,
        boolean logementCommun,
        boolean victimeFinanciairementDependante,
        boolean demandeurDejaProtege,
        List<String> demandeMesures,
        // SF-222-05 : DEC envisagé par l'avocat (suivi électronique du contact).
        boolean decEnvisage,
        int scoreVraisemblance,
        String verdictProbabiliteOctroi,
        List<String> mesuresRecommandees,
        int delaiTraitementJoursPrevisionnel,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
