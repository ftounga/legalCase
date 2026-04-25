package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-29-01 : résultat de l'analyse du crédit-temps belge persisté en JSON.
 *
 * <p>Mêmes champs que la réponse, sans le {@code caseFileId} (qui est porté par
 * l'entité parente).
 */
public record CreditTempsBeResult(
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
