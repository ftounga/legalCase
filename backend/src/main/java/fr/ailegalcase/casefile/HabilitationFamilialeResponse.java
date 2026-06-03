package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-222-03 : réponse API /api/v1/case-files/{id}/habilitation-familiale-analysis.
 */
public record HabilitationFamilialeResponse(
        UUID caseFileId,
        String verdict,
        String modalite,
        List<String> actesCouverts,
        List<String> conditionsManquantes,
        List<String> basesJuridiques,
        List<String> messages,
        String country
) {}
