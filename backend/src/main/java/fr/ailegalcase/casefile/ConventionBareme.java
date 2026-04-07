package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

/**
 * Barème d'ancienneté pour une convention collective (FR) ou commission paritaire (BE).
 */
public record ConventionBareme(
        String code,
        String label,
        String country,
        int congesLegauxJours,
        List<CongesSupplementaire> congesSupplementaires,
        List<PrimeAnciennete> primesAnciennete,
        String baseJuridique
) {
    /**
     * Congés supplémentaires par tranche d'ancienneté.
     */
    public record CongesSupplementaire(int ancienneteMinAnnees, int joursSupplementaires, String description) {}

    /**
     * Prime d'ancienneté par tranche.
     */
    public record PrimeAnciennete(int ancienneteMinAnnees, BigDecimal pourcentage, String description) {}
}
