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

    /**
     * @param conventionCode  code du barème
     * @param dateEntree      date d'entrée dans l'entreprise
     * @param salaireBase     salaire mensuel brut
     * @param congesContrat   nombre de jours de congés prévus au contrat
     * @param primeContrat    pourcentage de prime d'ancienneté prévu au contrat (nullable = 0)
     * @return le résultat complet du calcul
     */
    public static AncienneteResult calculate(
            String conventionCode,
            LocalDate dateEntree,
            BigDecimal salaireBase,
            int congesContrat,
            BigDecimal primeContrat
    ) {
        return calculate(conventionCode, dateEntree, salaireBase, congesContrat, primeContrat,
                ConventionBaremeReferentiel.getByCode(conventionCode));
    }

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

        int congesTotal = bareme.congesLegauxJours() + congesSupp;

        BigDecimal primePourcentage = bareme.primesAnciennete().stream()
                .filter(p -> annees >= p.ancienneteMinAnnees())
                .map(ConventionBareme.PrimeAnciennete::pourcentage)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal primeMontant = salaireBase.multiply(primePourcentage)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        BigDecimal primeContratEffective = primeContrat != null ? primeContrat : BigDecimal.ZERO;

        List<AncienneteResult.Ecart> ecarts = new ArrayList<>();

        if (congesContrat < congesTotal) {
            ecarts.add(new AncienneteResult.Ecart(
                    "Congés annuels",
                    congesTotal + " jours (légal + convention)",
                    congesContrat + " jours (contrat)",
                    AncienneteResult.Ecart.ECART
            ));
        } else {
            ecarts.add(new AncienneteResult.Ecart(
                    "Congés annuels",
                    congesTotal + " jours (légal + convention)",
                    congesContrat + " jours (contrat)",
                    AncienneteResult.Ecart.CONFORME
            ));
        }

        if (primeContratEffective.compareTo(primePourcentage) < 0) {
            ecarts.add(new AncienneteResult.Ecart(
                    "Prime d'ancienneté",
                    primePourcentage + "% (convention)",
                    primeContratEffective + "% (contrat)",
                    AncienneteResult.Ecart.ECART
            ));
        } else {
            ecarts.add(new AncienneteResult.Ecart(
                    "Prime d'ancienneté",
                    primePourcentage + "% (convention)",
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
                ecarts
        );
    }
}
