package fr.ailegalcase.casefile;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * SF-215-07 : body de la requête POST
 * {@code /api/v1/case-files/{id}/naturalisation-12bis-be-analysis}.
 *
 * <p>Tous les champs sont requis — la validation des bornes est portée par cette
 * annotation et par {@link Naturalisation12bisBeCalculator}.
 *
 * <p>Source : Code de la nationalité belge (loi 28/06/1984) art. 12bis +
 * AR 14/01/2013 (preuve langue) + AR 27/04/2018 (preuve intégration).
 */
public record Naturalisation12bisBeRequest(
        @NotNull @PositiveOrZero @Max(600) Integer dureeSejour,
        @NotNull NaturalisationBeTypeSejourEnum typeSejour,
        @NotNull NaturalisationBeNiveauLangueEnum niveauLangue,
        @NotNull Boolean preuveIntegration,
        @NotNull Boolean preuveEmploi,
        @NotNull Boolean menaceOrdrePublic,
        @NotNull Boolean condamnationPenale
) {}
