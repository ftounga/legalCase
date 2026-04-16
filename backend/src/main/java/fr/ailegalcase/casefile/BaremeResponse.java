package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.util.List;

public record BaremeResponse(
        String conventionCode,
        String conventionLabel,
        String country,
        int congesLegauxJours,
        List<CongesSupplementaireData> congesSupplementaires,
        List<PrimeAncienneteData> primesAnciennete
) {
    public record CongesSupplementaireData(int ancienneteMinAnnees, int joursSupplementaires) {}
    public record PrimeAncienneteData(int ancienneteMinAnnees, BigDecimal pourcentage) {}
}
