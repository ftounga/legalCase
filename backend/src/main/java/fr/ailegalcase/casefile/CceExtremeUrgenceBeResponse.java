package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-15 : payload de réponse HTTP pour
 * {@code /api/v1/case-files/{id}/cce-extreme-urgence-be-analysis}.
 */
public record CceExtremeUrgenceBeResponse(
        UUID caseFileId,
        LocalDate dateActeExecutoire,
        CceExtremeUrgenceBeTypeActeEnum typeActe,
        boolean recoursForme,
        LocalDate dateRecours,
        LocalDate dateLimiteRecours,
        long joursOuvrablesRestants,
        CceExtremeUrgenceBeStatut statut,
        LocalDate audienceEstimee,
        String actionImmediate,
        String recommandation,
        String baseJuridique
) {}
