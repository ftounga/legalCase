package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-215-15 : body de la requête POST
 * {@code /api/v1/case-files/{id}/cce-extreme-urgence-be-analysis}.
 *
 * <p>{@code dateRecours} est optionnel — requis uniquement si {@code recoursForme}
 * vaut {@code true} (validation portée par {@link CceExtremeUrgenceBeCalculator}).
 */
public record CceExtremeUrgenceBeRequest(
        @NotNull LocalDate dateActeExecutoire,
        @NotNull CceExtremeUrgenceBeTypeActeEnum typeActe,
        @NotNull Boolean recoursForme,
        LocalDate dateRecours
) {}
