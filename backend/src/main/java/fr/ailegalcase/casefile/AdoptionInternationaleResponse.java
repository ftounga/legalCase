package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-17 : réponse API /api/v1/case-files/{id}/adoption-internationale.
 */
public record AdoptionInternationaleResponse(
        UUID caseFileId,
        boolean conditionsRemplies,
        VoieProcedureAdoptionEnum voieProcedure,
        boolean conventionApplicable,
        boolean alerteKafala,
        boolean exequaturRequis,
        String delaiEstime,
        String verdict,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
