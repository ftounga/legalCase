package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-21-01 : résultat du calcul d'indemnité forfaitaire pour travail dissimulé
 * (art. L.8223-1 Code du travail).
 */
public record TravailDissimuleResult(
        BigDecimal salaireMensuelReference,
        BigDecimal indemniteForfaitaire,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
