package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-19 : réponse API /api/v1/case-files/{id}/indignite-successorale.
 */
public record IndigniteSuccessoraleResponse(
        UUID caseFileId,
        String typeIndignite,
        String verdictIndignite,
        boolean pardonNeutralisant,
        boolean representationPossible,
        String delaiAction,
        boolean delaiForclos,
        String effetDevolution,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
