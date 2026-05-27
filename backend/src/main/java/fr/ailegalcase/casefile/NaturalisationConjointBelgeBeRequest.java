package fr.ailegalcase.casefile;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

/**
 * SF-215-09 : body de la requête POST
 * {@code /api/v1/case-files/{id}/naturalisation-conjoint-belge-be-analysis}.
 *
 * <p>Tous les champs sont requis — la validation des bornes est portée par cette
 * annotation et par {@link NaturalisationConjointBelgeBeCalculator}.
 *
 * <p>Source : Code de la nationalité belge (loi 28/06/1984) art. 16 §1 2°
 * + AR 14/01/2013 (preuve langue) + loi modificative 04/12/2012.
 */
public record NaturalisationConjointBelgeBeRequest(
        @NotNull
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
        LocalDate dateMarriage,
        @NotNull Boolean cohabitationLegale,
        @NotNull @PositiveOrZero @Max(600) Integer dureeCohabitationMois,
        @NotNull NaturalisationBeNiveauLangueEnum niveauLangue,
        @NotNull Boolean preuveIntegration,
        @NotNull Boolean menaceOrdrePublic,
        @NotNull Boolean condamnationPenale
) {}
