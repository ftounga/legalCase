package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-16-01 : réponse de l'endpoint de détection licenciement nul.
 *
 * <p>Inclut un snapshot complet des inputs (pour pré-remplissage UI) ET les sorties calculées.</p>
 */
public record LicenciementNulDetectionResponse(
        UUID caseFileId,
        // --- Inputs (snapshot pour pré-remplissage UI) ---
        LocalDate dateNotificationLicenciement,
        Boolean salarieEnceinte,
        LocalDate dateAccouchement,
        Boolean salarieAccidentTravail,
        LocalDate dateConsolidationAT,
        Boolean salarieHarceleAvere,
        Boolean salarieDiscriminationAlleguee,
        Boolean salarieMotifLanceurAlerte,
        Boolean salarieMandatRepresentant,
        Boolean salarieActionJustice,
        BigDecimal salaireMensuelBrutEur,
        Integer ancienneteAnnees,
        // --- Outputs calculés ---
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
