package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-13 : réponse API /api/v1/case-files/{id}/audition-mineur.
 */
public record AuditionMineurResponse(
        UUID caseFileId,
        boolean conditionsRemplies,
        boolean droitAuditionReconnu,
        ModaliteAuditionEnum modaliteRecommandee,
        boolean refusContestable,
        String verdict,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
