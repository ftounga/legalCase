package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * SF-DT-17-01 : résultat du calcul d'indemnité de précarité CDD
 * (art. L.1243-8 Code du travail).
 */
public record IndemnitePrecariteCddResult(
        BigDecimal totalSalairesBruts,
        int tauxPrecarite,
        String casExclusion,
        BigDecimal indemnitePrecarite,
        String formule,
        String baseJuridique,
        List<String> messages
) {}
