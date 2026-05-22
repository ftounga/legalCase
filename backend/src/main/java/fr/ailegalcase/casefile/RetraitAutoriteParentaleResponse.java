package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-11 : réponse API /api/v1/case-files/{id}/retrait-autorite-parentale.
 */
public record RetraitAutoriteParentaleResponse(
        UUID caseFileId,
        VerdictRetraitApEnum verdictRetrait,
        VoieProceduraleRetraitApEnum voieProcedurale,
        boolean admissibiliteAdoption,
        List<String> consequencesJuridiques,
        List<String> etapes,
        int dureeEstimeeJours,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
