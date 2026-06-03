package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-222-04 : réponse API /api/v1/case-files/{id}/assistance-educative-analysis.
 */
public record AssistanceEducativeResponse(
        UUID caseFileId,
        String verdict,
        String juridiction,
        String mesureOrientee,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
