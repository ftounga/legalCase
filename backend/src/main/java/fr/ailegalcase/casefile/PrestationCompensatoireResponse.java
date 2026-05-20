package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

/**
 * SF-216-01 : réponse API /api/v1/case-files/{id}/prestation-compensatoire.
 */
public record PrestationCompensatoireResponse(
        UUID caseFileId,
        int montantCapitalIndicatifEur,
        int renteIndicativeMensuelleEur,
        FormePrestationEnum formeRecommandee,
        String dispAriteNiveauxVie,
        String baseJuridique,
        List<String> messages,
        List<String> alertes,
        String country
) {}
