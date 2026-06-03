package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-221-04 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/detention-centre-ferme-be-analysis}.
 */
public record DetentionCentreFermeBeResponse(
        UUID caseFileId,
        LocalDate dateDebutDetention,
        DetentionBaseLegale baseLegaleDetention,
        boolean prolongationNotifiee,
        LocalDate dateProlongation,
        boolean requeteMiseEnLiberteDeposee,
        LocalDate dateNotificationDecisionDetention,
        DetentionCentreFermeBeVerdict verdict,
        int dureeDetentionJours,
        LocalDate dateLimiteRequete,
        Integer joursRestantsRequete,
        List<String> basesJuridiques,
        List<String> messages
) {}
