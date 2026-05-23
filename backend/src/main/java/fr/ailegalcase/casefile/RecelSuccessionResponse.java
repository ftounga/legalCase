package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-21 : réponse API /api/v1/case-files/{id}/recel-succession.
 */
public record RecelSuccessionResponse(
        UUID caseFileId,
        String qualificationRecel,
        String verdictRecel,
        String sanctionCivile,
        String preuveRequise,
        String delaiAction,
        boolean delaiForclos,
        boolean voletPenalSignale,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
