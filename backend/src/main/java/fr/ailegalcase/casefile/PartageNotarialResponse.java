package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-216-27 : réponse API /api/v1/case-files/{id}/partage-notarial.
 */
public record PartageNotarialResponse(
        UUID caseFileId,
        boolean notaireObligatoire,
        List<String> calendrierEtapes,
        LocalDate delaiDeclarationFiscale,
        boolean alerteDelai,
        boolean orientationJudiciaire,
        String baseLegale,
        List<String> messages,
        List<String> alertes,
        String country
) {}
