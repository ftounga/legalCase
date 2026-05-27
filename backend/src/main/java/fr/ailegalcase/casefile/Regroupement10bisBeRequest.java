package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * SF-215-05 : body de la requête POST {@code /api/v1/case-files/{id}/regroupement-10bis-be-analysis}.
 * Tous les champs sont requis — la validation des bornes est portée par cette annotation et par
 * {@link Regroupement10bisBeCalculator}.
 *
 * <p>Différence majeure vs {@link Regroupement10terBeRequest} : présence du champ
 * {@code dateFinCarteA} (date d'expiration du titre A du regroupant) — sans cette date,
 * impossible de vérifier la validité du titre permettant le regroupement (règle dure).
 *
 * <p>Note : revenus du regroupant uniquement (simplification V1 — pas de revenus du ménage entier).
 */
public record Regroupement10bisBeRequest(
        @NotNull Regroupement10bisBeLienFamilialEnum lienFamilial,
        @NotNull Regroupement10bisBeTypeCarteEnum typeCarteRegroupant,
        @NotNull @PositiveOrZero @Max(100_000) Integer revenusMensuelsNetsRegroupant,
        @NotNull @PositiveOrZero @Max(600) Integer dureeSejour,
        @NotNull LocalDate dateFinCarteA,
        @NotNull Boolean logementConforme,
        @NotNull Boolean assuranceMaladie,
        @NotNull Boolean menaceOrdrePublic
) {}
