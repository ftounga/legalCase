package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-11-01 : réponse de l'endpoint {@code /harcelement-licenciement-nul}.
 */
public record HarcelementNulliteResponse(
        UUID caseFileId,
        BigDecimal salaireMensuelReference,
        HarcelementNulliteCalculator.MotifNullite motifNullite,
        String country,
        BigDecimal indemniteMinimumNullite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
