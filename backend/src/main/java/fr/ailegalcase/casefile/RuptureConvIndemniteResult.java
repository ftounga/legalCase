package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Résultat du calcul d'indemnité minimum légale de rupture conventionnelle (art. R1234-2).
 */
public record RuptureConvIndemniteResult(
        int ancienneteAnnees,
        BigDecimal salaireMensuel,
        BigDecimal indemniteLegaleMinimum,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
