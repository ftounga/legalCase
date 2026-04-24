package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-15-01 : réponse de l'endpoint {@code /inaptitude}.
 */
public record InaptitudeResponse(
        UUID caseFileId,
        BigDecimal salaireMensuelReference,
        int ancienneteAnnees,
        InaptitudeCalculator.OrigineInaptitude origineInaptitude,
        boolean reclassementRespecte,
        LocalDate avisMedecinTravailDate,
        String country,
        BigDecimal indemniteLegale,
        BigDecimal indemniteCompensatricePreavis,
        BigDecimal damagesReclassement,
        BigDecimal total,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
