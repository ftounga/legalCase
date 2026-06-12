package fr.ailegalcase.casefile;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * F-285 / SF-285-01 — corps du PUT de qualification d'entrée d'un dossier.
 * Tous les champs sont optionnels (qualification partielle possible). La cohérence
 * applicative (≥ 0, énums valides) est portée par la validation déclarative + le service.
 */
public record CaseIntakeRequest(
        @Size(max = 120, message = "disputeType must not exceed 120 characters")
        String disputeType,

        AdmissibilityStatus admissibility,

        PrescriptionStatus prescriptionStatus,

        LocalDate prescriptionDeadline,

        @Size(max = 120, message = "jurisdiction must not exceed 120 characters")
        String jurisdiction,

        @PositiveOrZero(message = "estimatedValueCents must be zero or positive")
        Long estimatedValueCents,

        @Size(max = 1000, message = "notes must not exceed 1000 characters")
        String notes
) {}
