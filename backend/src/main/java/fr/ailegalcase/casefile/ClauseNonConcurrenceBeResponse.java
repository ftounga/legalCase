package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * SF-213-01 : réponse REST de l'endpoint
 * {@code /api/v1/case-files/{caseFileId}/decision-tools/clause-non-concurrence-be}.
 */
public record ClauseNonConcurrenceBeResponse(
        UUID caseFileId,
        BigDecimal remunerationAnnuelleBrute,
        int dureeMois,
        ClauseNonConcurrenceBeZoneEnum zoneGeographique,
        boolean activiteInternationaleProuvee,
        BigDecimal salaireAnnuelSeuil,
        ClauseNonConcurrenceBeVerdict verdict,
        ClauseNonConcurrenceBeRaisonNullite raisonNullite,
        BigDecimal indemniteLegale,
        String indemniteLegaleFormule,
        String baseJuridique,
        String avertissement
) {}
