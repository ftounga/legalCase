package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-09 : réponse API /api/v1/case-files/{id}/delegation-ap-fr.
 */
public record DelegationApFrResponse(
        UUID caseFileId,
        VerdictRecevabiliteDelegationApEnum verdictRecevabilite,
        TypeDelegationApEnum voieProcedurale,
        List<String> etapes,
        int dureeEstimeeJours,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
