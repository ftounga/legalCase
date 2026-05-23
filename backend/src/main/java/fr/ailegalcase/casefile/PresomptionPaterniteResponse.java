package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-25 : réponse API
 * /api/v1/case-files/{id}/presomption-paternite.
 */
public record PresomptionPaterniteResponse(
        UUID caseFileId,
        boolean presomptionApplicable,
        boolean presomptionRenversee,
        String voieDesaveu,
        String delaiDesaveu,
        String possessionEtatImpact,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
