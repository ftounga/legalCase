package fr.ailegalcase.casefile;

import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-07 : réponse de l'analyse du délai de validation du VLS-TS auprès de
 * l'OFII (art. R. 311-3 CESEDA). Outil single-country FR.
 */
public record VlsTsValidationResponse(
        UUID caseFileId,
        LocalDate dateEntreeFrance,
        VlsTsValidationTypeEnum typeVlsTs,
        boolean validationOFIIEffectuee,
        LocalDate dateValidationOFII,
        LocalDate dateEcheanceValidation,
        Long joursRestantsValidation,
        VlsTsValidationStatut statut,
        boolean risqueIrregularite,
        String procedureRecours,
        String recommandation,
        String country,
        String baseJuridique
) {}
