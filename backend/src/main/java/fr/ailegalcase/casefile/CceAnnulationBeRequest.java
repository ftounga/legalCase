package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-215-13 : body de la requête POST
 * {@code /api/v1/case-files/{id}/cce-annulation-be-analysis}.
 *
 * <p>{@code dateRecours} est optionnel — requis uniquement si {@code recoursForme}
 * vaut {@code true} (validation portée par {@link CceAnnulationBeCalculator}).
 */
public record CceAnnulationBeRequest(
        @NotNull LocalDate dateNotificationDecision,
        @NotNull CceAnnulationBeTypeDecisionEnum typeDecision,
        @NotNull Boolean recoursForme,
        LocalDate dateRecours
) {}
