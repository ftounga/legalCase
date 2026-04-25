package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-29-01 : réponse de l'analyse du crédit-temps belge.
 */
public record CreditTempsBeResponse(
        UUID caseFileId,
        String regime,
        boolean eligible,
        int scoreGlobal,
        String verdictEligibilite,
        List<String> criteresNonRemplis,
        BigDecimal indemniteOnemMensuelle,
        int dureeMaximaleMois,
        String baseJuridique,
        String formule,
        List<String> messages
) {}
