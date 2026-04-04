package fr.ailegalcase.analysis;

import java.util.Optional;

/**
 * Calcule la pension alimentaire indicative selon le barème UNAF simplifié (France)
 * et le barème de référence CGKR (Belgique).
 *
 * Barème France (UNAF — % du revenu net mensuel du débiteur) :
 *   1 enfant : 18 % (exclusive) / 11 % (alternée)
 *   2 enfants : 26 % / 16 %
 *   3 enfants : 30 % / 19 %
 *   4 enfants : 33 % / 21 %
 *   5+ enfants : 35 % / 22 %
 *
 * Barème Belgique (CGKR simplifié) :
 *   1 enfant : 15 % / 9 %
 *   2 enfants : 22 % / 14 %
 *   3 enfants : 27 % / 17 %
 *   4 enfants : 31 % / 19 %
 *   5+ enfants : 33 % / 21 %
 *
 * Le montant retourné est une fourchette indicative ± 10 %.
 * AVERTISSEMENT : résultat indicatif uniquement, ne constitue pas un avis juridique.
 */
public final class PensionAlimentaireCalculator {

    public record PensionAlimentaireEstimate(
            double montantMin,
            double montantMax,
            double revenus,
            int nbEnfants,
            String modeGarde,
            String pays,
            boolean donneesPartielles
    ) {}

    private static final double[][] TAUX_FRANCE = {
            // {garde exclusive, garde alternée}
            {0.18, 0.11},
            {0.26, 0.16},
            {0.30, 0.19},
            {0.33, 0.21},
            {0.35, 0.22},
    };

    private static final double[][] TAUX_BELGIQUE = {
            {0.15, 0.09},
            {0.22, 0.14},
            {0.27, 0.17},
            {0.31, 0.19},
            {0.33, 0.21},
    };

    private PensionAlimentaireCalculator() {}

    public static Optional<PensionAlimentaireEstimate> calculate(
            Double revenus, Integer nbEnfants, String modeGarde, String pays) {

        if (revenus == null && nbEnfants == null) return Optional.empty();

        boolean donneesPartielles = (revenus == null || revenus <= 0 || nbEnfants == null);

        double safeRevenus = (revenus != null && revenus > 0) ? revenus : 0;
        int safeNbEnfants  = (nbEnfants != null && nbEnfants > 0) ? nbEnfants : 1;
        String safePays    = "BELGIQUE".equalsIgnoreCase(pays) ? "BELGIQUE" : "FRANCE";
        boolean alternee   = "ALTERNEE".equalsIgnoreCase(modeGarde);

        double[][] taux = "BELGIQUE".equals(safePays) ? TAUX_BELGIQUE : TAUX_FRANCE;
        int idx = Math.min(safeNbEnfants, 5) - 1;
        double tauxApplique = alternee ? taux[idx][1] : taux[idx][0];

        double montant    = Math.round(safeRevenus * tauxApplique * 100.0) / 100.0;
        double montantMin = Math.round(montant * 0.90 * 100.0) / 100.0;
        double montantMax = Math.round(montant * 1.10 * 100.0) / 100.0;

        return Optional.of(new PensionAlimentaireEstimate(
                montantMin,
                montantMax,
                safeRevenus,
                safeNbEnfants,
                alternee ? "ALTERNEE" : "EXCLUSIVE",
                safePays,
                donneesPartielles
        ));
    }
}
