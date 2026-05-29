package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-215-17 : body de la requête POST
 * {@code /api/v1/case-files/{id}/annexe13quinquies-be-analysis}.
 *
 * <p>{@code dateRecours} est optionnel — requis uniquement si {@code recoursForme}
 * vaut {@code true} (validation portée par {@link Annexe13quinquiesBeCalculator}).
 */
public record Annexe13quinquiesBeRequest(
        @NotNull LocalDate dateNotificationAnnexe,
        @NotNull Annexe13quinquiesBeMotifEnum motifInterdictionEntree,
        @NotNull Boolean precedentSejour,
        @NotNull Boolean recoursForme,
        LocalDate dateRecours
) {}
