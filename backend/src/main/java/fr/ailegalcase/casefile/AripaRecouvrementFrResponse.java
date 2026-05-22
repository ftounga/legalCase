package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-07 : réponse API /api/v1/case-files/{id}/aripa-recouvrement-fr.
 */
public record AripaRecouvrementFrResponse(
        UUID caseFileId,
        VoieRecouvrementAripaEnum voieRecommandee,
        int montantArrieres,
        int montantAsfEligibleMensuelEur,
        int delaiEstimeJours,
        List<String> etapes,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
