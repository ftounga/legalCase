package fr.ailegalcase.casefile;

import java.time.LocalDate;

/**
 * SF-214-07 : requête POST pour l'analyse du délai de validation du VLS-TS
 * auprès de l'OFII (art. R. 311-3 CESEDA). Outil single-country FR.
 */
public record VlsTsValidationRequest(
        LocalDate dateEntreeFrance,
        VlsTsValidationTypeEnum typeVlsTs,
        Boolean validationOFIIEffectuee,
        LocalDate dateValidationOFII
) {}
