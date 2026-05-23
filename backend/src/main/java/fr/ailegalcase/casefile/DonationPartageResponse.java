package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-29 : réponse API /api/v1/case-files/{id}/donation-partage.
 */
public record DonationPartageResponse(
        UUID caseFileId,
        boolean conditionsRemplies,
        String interet,
        String gelValeurEffet,
        boolean rapportExclu,
        boolean alerteQuotite,
        List<String> etapesNotariales,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
