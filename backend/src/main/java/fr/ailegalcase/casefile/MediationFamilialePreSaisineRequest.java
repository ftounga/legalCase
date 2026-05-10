package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-210-01 : requête POST {@code /api/v1/case-files/{id}/mediation-familiale-pre-saisine}.
 */
public record MediationFamilialePreSaisineRequest(
        MediationFamilialePreSaisineMotif motifSaisine,
        Boolean mediationTentee,
        LocalDate dateMediation,
        MediationFamilialePreSaisineException exceptionApplicable,
        String exceptionDetail
) {}
