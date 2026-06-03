package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * SF-221-04 : body de la requête POST
 * {@code /api/v1/case-files/{id}/detention-centre-ferme-be-analysis}.
 *
 * <p>Détention administrative en centre fermé (Loi 15/12/1980 art. 7 al. 3 / 27 / 29 /
 * 74/5 ; AR 02/08/2002) et requête de mise en liberté devant la chambre du conseil
 * (art. 71 et s. ; fenêtre indicative 5 jours).
 */
public record DetentionCentreFermeBeRequest(
        @NotNull LocalDate dateDebutDetention,
        @NotNull DetentionBaseLegale baseLegaleDetention,
        @NotNull Boolean prolongationNotifiee,
        LocalDate dateProlongation,
        @NotNull Boolean requeteMiseEnLiberteDeposee,
        LocalDate dateNotificationDecisionDetention
) {}
