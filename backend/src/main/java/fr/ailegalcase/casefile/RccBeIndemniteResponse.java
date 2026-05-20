package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-207-07 : projection complète (snapshot) renvoyée au client pour
 * l'outil indemnité complémentaire RCC BE.
 *
 * <p>Inclut les inputs (pour pré-fill UI) et les outputs calculés
 * (cf. {@link RccBeIndemniteResult}).</p>
 */
public record RccBeIndemniteResponse(
        UUID caseFileId,
        // Inputs normalisés (pré-fill UI)
        BigDecimal remunerationNetteReference,
        BigDecimal allocationOnemMensuelle,
        LocalDate dateNaissanceSalarie,
        LocalDate dateDebutRcc,
        int ageLegalPensionApplique,
        BigDecimal planchersSectoriel,
        // Outputs calculés
        BigDecimal indemniteMensuelleAvantPlancher,
        BigDecimal indemniteMensuelleEmployeur,
        boolean planchersSectorielApplique,
        LocalDate dateAgeLegalPension,
        long moisRestantsJusquaPension,
        BigDecimal montantTotalEmployeur,
        String baseJuridique,
        String formuleCalcul,
        // Workspace context (toujours BELGIQUE pour cet outil)
        String country
) {}
