package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-11-01 : résultat du calcul d'indemnité minimum pour licenciement nul
 * (harcèlement / discrimination / violence au travail — FR + BE).
 */
public record HarcelementNulliteResult(
        BigDecimal salaireMensuelReference,
        HarcelementNulliteCalculator.MotifNullite motifNullite,
        String country,
        BigDecimal indemniteMinimumNullite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
