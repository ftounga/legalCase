package fr.ailegalcase.casefile;

import java.math.BigDecimal;

/**
 * Entrée du barème d'indemnités : plancher et plafond en mois de salaire pour une ancienneté donnée.
 */
public record IndemniteBareme(
        int ancienneteAnnees,
        BigDecimal plancherMois,
        BigDecimal plafondMois
) {}
