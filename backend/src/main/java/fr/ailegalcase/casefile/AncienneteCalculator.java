package fr.ailegalcase.casefile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcule l'ancienneté, les congés acquis, la prime d'ancienneté et les écarts avec le contrat.
 */
public final class AncienneteCalculator {

    private AncienneteCalculator() {}

    public static AncienneteResult calculate(
            String conventionCode,
            LocalDate dateEntree,
            BigDecimal salaireBase,
            int congesContrat,
            BigDecimal primeContrat,
            ConventionBareme bareme
    ) {
        if (bareme == null) {
            throw new IllegalArgumentException("Convention inconnue : " + conventionCode);
        }

        Period period = Period.between(dateEntree, LocalDate.now());
        int annees = period.getYears();
        int mois = period.getMonths();

        int congesSupp = bareme.congesSupplementaires().stream()
                .filter(c -> annees >= c.ancienneteMinAnnees())
                .mapToInt(ConventionBareme.CongesSupplementaire::joursSupplementaires)
                .max()
                .orElse(0);

        int congesBareme = bareme.congesLegauxJours() + congesSupp;
        // Le contrat individuel ne peut pas être moins favorable. Si plus, c'est lui qui s'applique.
        int congesTotal = Math.max(congesContrat, congesBareme);

        BigDecimal primeBareme = bareme.primesAnciennete().stream()
                .filter(p -> annees >= p.ancienneteMinAnnees())
                .map(ConventionBareme.PrimeAnciennete::pourcentage)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal primeContratEffective = primeContrat != null ? primeContrat : BigDecimal.ZERO;
        // Le contrat individuel ne peut pas être moins favorable que la convention.
        // Si le contrat est plus favorable, c'est cette valeur qui s'applique.
        BigDecimal primePourcentage = primeContratEffective.max(primeBareme);

        BigDecimal primeMontant = salaireBase.multiply(primePourcentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        List<AncienneteResult.Ecart> ecarts = new ArrayList<>();

        if (congesContrat < congesBareme) {
            ecarts.add(new AncienneteResult.Ecart(
                    "Congés annuels",
                    congesBareme + " jours (légal + convention)",
                    congesContrat + " jours (contrat)",
                    AncienneteResult.Ecart.ECART
            ));
        } else {
            ecarts.add(new AncienneteResult.Ecart(
                    "Congés annuels",
                    congesBareme + " jours (légal + convention)",
                    congesContrat + " jours (contrat)",
                    AncienneteResult.Ecart.CONFORME
            ));
        }

        if (primeContratEffective.compareTo(primeBareme) < 0) {
            ecarts.add(new AncienneteResult.Ecart(
                    "Prime d'ancienneté",
                    primeBareme + "% (convention)",
                    primeContratEffective + "% (contrat)",
                    AncienneteResult.Ecart.ECART
            ));
        } else {
            ecarts.add(new AncienneteResult.Ecart(
                    "Prime d'ancienneté",
                    primeBareme + "% (convention)",
                    primeContratEffective + "% (contrat)",
                    AncienneteResult.Ecart.CONFORME
            ));
        }

        return new AncienneteResult(
                conventionCode,
                bareme.label(),
                bareme.country(),
                annees,
                mois,
                bareme.congesLegauxJours(),
                congesSupp,
                congesTotal,
                primePourcentage,
                primeMontant,
                primeBareme,
                congesBareme,
                ecarts
        );
    }
}
