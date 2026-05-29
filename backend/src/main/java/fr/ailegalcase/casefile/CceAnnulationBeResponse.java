package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-13 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/cce-annulation-be-analysis}.
 */
public record CceAnnulationBeResponse(
        UUID caseFileId,
        LocalDate dateNotificationDecision,
        CceAnnulationBeTypeDecisionEnum typeDecision,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateLimiteRecours,
        long joursRestants,
        CceAnnulationBeStatut statut,
        String recommandation,
        String delaisMemoire,
        String baseJuridique
) {}
