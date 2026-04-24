package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * SF-DT-15-01 : résultat du calcul d'indemnité de licenciement pour inaptitude (FR + BE).
 */
public record InaptitudeResult(
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
