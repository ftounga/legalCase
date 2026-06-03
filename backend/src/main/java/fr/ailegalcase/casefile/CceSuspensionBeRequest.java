package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-221-05 : body de la requête POST
 * {@code /api/v1/case-files/{id}/cce-suspension-be-analysis}.
 *
 * <p>Recours CCE en SUSPENSION ORDINAIRE (référé administratif : art. 39/82 Loi
 * 15/12/1980 ; loi 15/09/2006). Conditions : urgence (non extrême) + risque de
 * préjudice grave difficilement réparable, greffé sur la requête en annulation 30j.
 */
public record CceSuspensionBeRequest(
        @NotNull LocalDate dateNotificationDecision,
        @NotNull Boolean recoursAnnulationIntroduit,
        @NotNull Boolean urgenceInvocable,
        @NotNull Boolean risquePrejudiceGraveDifficilementReparable,
        @NotNull Boolean mesureEloignementImminente
) {}
