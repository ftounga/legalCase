package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-15 : réponse API /api/v1/case-files/{id}/adoption-intra-fr.
 */
public record AdoptionIntraFrResponse(
        UUID caseFileId,
        List<FormeAdoptionEnum> formesPossibles,
        FormeAdoptionEnum formeDemandee,
        boolean conditionsRemplies,
        boolean alerteIrreversibilite,
        boolean consentementEnfantRequis,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
