package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-222-01 : réponse API /api/v1/case-files/{id}/asf-caf-analysis.
 */
public record AsfCafResponse(
        UUID caseFileId,
        String verdict,
        int montantMensuelEstime,
        boolean recouvrementApplicable,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
