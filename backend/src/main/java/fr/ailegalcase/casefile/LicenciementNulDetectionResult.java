package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-16-01 : résultat structuré de la détection licenciement nul (FR).
 */
public record LicenciementNulDetectionResult(
        List<LicenciementNulDetectionCalculator.Protection> protectionsDetectees,
        int nombreProtectionsActives,
        boolean nulliteProbable,
        int scoreNullite,
        LicenciementNulDetectionCalculator.VerdictProbabilite verdictProbabiliteNullite,
        BigDecimal indemniteMinimumNuliteEur,
        int indemniteMinimumMois,
        boolean reintegrationOuverte,
        String baseJuridique,
        String formule,
        List<String> messages,
        String country
) {}
