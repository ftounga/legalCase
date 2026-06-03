package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-221-05 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/cce-suspension-be-analysis}.
 */
public record CceSuspensionBeResponse(
        UUID caseFileId,
        LocalDate dateNotificationDecision,
        boolean recoursAnnulationIntroduit,
        boolean urgenceInvocable,
        boolean risquePrejudiceGraveDifficilementReparable,
        boolean mesureEloignementImminente,
        CceSuspensionBeVerdict verdict,
        int joursDepuisNotification,
        String renvoiOutil,
        List<String> basesJuridiques,
        List<String> messages
) {}
