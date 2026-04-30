package fr.ailegalcase.casefile;

/**
 * Critère binaire de validité d'un dossier immigration FR/BE (F-IM-21).
 * Pattern miroir F-DT-08 (validité licenciement) — VERIFIED / NON_COMPLIANT / TO_CHECK.
 */
public record ImmigrationValidationCriterion(
        String code,
        String country,
        String label,
        String baseJuridique,
        String description
) {}
