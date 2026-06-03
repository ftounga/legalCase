package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-222-02 : réponse API /api/v1/case-files/{id}/tgd-analysis.
 */
public record TgdResponse(
        UUID caseFileId,
        String verdict,
        List<String> criteresManquants,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
