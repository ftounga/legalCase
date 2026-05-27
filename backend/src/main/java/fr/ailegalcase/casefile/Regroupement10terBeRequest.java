package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * SF-215-03 : body de la requête POST {@code /api/v1/case-files/{id}/regroupement-10ter-be-analysis}.
 * Tous les champs sont requis — la validation des bornes est portée par cette annotation et par
 * {@link Regroupement10terBeCalculator}.
 *
 * <p>Note : revenus du regroupant uniquement (simplification V1 — pas de revenus du ménage entier).
 */
public record Regroupement10terBeRequest(
        @NotNull Regroupement10terBeLienFamilialEnum lienFamilial,
        @NotNull Regroupement10terBeTypeCarteEnum typeCarteRegroupant,
        @NotNull @PositiveOrZero @Max(100_000) Integer revenusMensuelsNetsRegroupant,
        @NotNull @PositiveOrZero @Max(600) Integer dureeSejour,
        @NotNull Boolean logementConforme,
        @NotNull Boolean assuranceMaladie,
        @NotNull Boolean menaceOrdrePublic
) {}
