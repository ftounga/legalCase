package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-23 : réponse API /api/v1/case-files/{id}/donation-entre-epoux.
 */
public record DonationEntreEpouxResponse(
        UUID caseFileId,
        String validite,
        String revocabilite,
        String effetDissolution,
        boolean actionRetranchementPossible,
        String impactReserve,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
