package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * SF-DT-17-01 : requête de calcul de l'indemnité de précarité CDD.
 *
 * @param totalSalairesBruts somme des rémunérations brutes perçues pendant le CDD (>0)
 * @param tauxPrecarite      10 (défaut) ou 6 (convention avec contrepartie, L.1243-9)
 * @param casExclusion       code d'exclusion de l'art. L.1243-10 (nullable)
 */
public record IndemnitePrecariteCddRequest(
        BigDecimal totalSalairesBruts,
        Integer tauxPrecarite,
        String casExclusion
) {}
