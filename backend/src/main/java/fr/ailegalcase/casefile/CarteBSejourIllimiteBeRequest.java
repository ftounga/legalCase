package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-221-02 : body de la requête POST
 * {@code /api/v1/case-files/{id}/carte-b-sejour-illimite-be-analysis}.
 *
 * <p>Passage carte A → carte B (séjour ILLIMITÉ d'un ressortissant tiers,
 * art. 14 Loi 15/12/1980) après 5 ans (= 60 mois) de séjour régulier ininterrompu.
 */
public record CarteBSejourIllimiteBeRequest(
        @NotNull LocalDate dateDebutSejourRegulier,
        @NotNull Boolean sejourIninterrompu,
        @NotNull Boolean absencesSuperieuresLimites,
        @NotNull Boolean motifSejourStable,
        @NotNull Boolean ordrePublicRisque
) {}
