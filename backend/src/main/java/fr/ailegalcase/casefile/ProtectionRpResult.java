package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-30-01 : résultat structuré du calcul de régularité de la procédure
 * de licenciement d'un salarié protégé (FR — art. L.2411-1 et s.).
 */
public record ProtectionRpResult(
        ProtectionRpCalculator.StatutProtege statutProtege,
        LocalDate dateExpirationMandat,
        LocalDate datePresumeeRupture,
        ProtectionRpCalculator.ProcedureSuivie procedureSuivie,
        ProtectionRpCalculator.MotifLicenciement motifLicenciement,
        double salaireMensuelBrutEur,
        String country,
        boolean salarieEncoreProtege,
        int scoreConformite,
        ProtectionRpCalculator.VerdictLegalite verdictLegalite,
        List<ProtectionRpCalculator.CritereRegularite> criteresRemplis,
        List<ProtectionRpCalculator.CritereRegularite> criteresManquants,
        double indemniteForfaitaireMinEur,
        double salaireEvictionPotentielEur,
        int delaiContestationJours,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
