package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-14-01 : résultat structuré du calcul de validité d'un PSE
 * (FR — art. L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1).
 */
public record PseResult(
        int tailleEntrepriseSalaries,
        int nombreLicenciementsEnvisages,
        int periodeJours,
        PseCalculator.ModeAdoption modeAdoption,
        PseCalculator.AvisCse csaeConsulteAvis,
        PseCalculator.StatutDreets dreetsStatut,
        LocalDate dateNotificationDreets,
        List<PseCalculator.ContenuMesure> contenuMesures,
        LocalDate dateProjet,
        String country,
        boolean pseRequis,
        int scoreConformite,
        PseCalculator.VerdictValidite verdictValidite,
        List<PseCalculator.CritereValidite> criteresRemplis,
        List<PseCalculator.CritereValidite> criteresManquants,
        int delaiContestationJours,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
