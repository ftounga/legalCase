package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-207-06 : projection complète (snapshot) renvoyée au client pour
 * l'outil d'éligibilité RCC BE.
 *
 * <p>Inclut les inputs (pour pré-fill UI) et les outputs calculés
 * (cf. {@link RccBeConditionsResult}).</p>
 */
public record RccBeConditionsResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        LocalDate dateNaissance,
        int anneesCarriereProfessionnelle,
        boolean metierLourd,
        boolean longueCarriere,
        boolean entrepriseEnDifficulte,
        LocalDate dateLicenciementEnvisagee,
        // Outputs calculés
        RccBeConditionsResult.Verdict verdict,
        RccBeConditionsRegime regimeApplicable,
        List<RccBeConditionsRegime> regimesEligibles,
        int ageALaDateLicenciement,
        int anneesCarriereCalculees,
        List<RccBeConditionsCondition> conditionsManquantes,
        String baseJuridique,
        String formuleCalcul,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
