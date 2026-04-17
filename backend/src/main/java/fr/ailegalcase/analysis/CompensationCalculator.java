package fr.ailegalcase.analysis;

import java.util.Optional;
import java.util.Set;

/**
 * Calcule l'indemnité légale de rupture et le plafond prud'homal (barème Macron)
 * selon le Code du travail (Art. R1234-2).
 *
 * Formule indemnité légale :
 *   - 1/4 de mois de salaire de référence par année pour les 10 premières années
 *   - 1/3 de mois par année au-delà de 10 ans
 *
 * Barème Macron (entreprises ≥ 11 salariés) — plafond dommages-intérêts.
 */
public final class CompensationCalculator {

    public record CompensationEstimate(
            double indemnite,
            double salaireReference,
            int ancienneteAnnees,
            int ancienneteMois,
            String typeRupture,
            int plafondMinMois,
            double plafondMaxMois,
            boolean donneesPartielles
    ) {}

    // Types Macron-compatibles (licenciement abusif / économique) — calcul complet possible
    private static final Set<String> MACRON_TYPES = Set.of(
            "LICENCIEMENT", "LICENCIEMENT_ECONOMIQUE", "LICENCIEMENT_ORDINAIRE"
    );

    // Enum étendu F-DT-09 : types pour lesquels on remonte un estimate partiel
    // (pré-remplissage + cohérence F-IA-03) sans calculer de plafond Macron.
    private static final Set<String> EXTENDED_TYPES = Set.of(
            "LICENCIEMENT", "LICENCIEMENT_ECONOMIQUE", "RUPTURE_CONVENTIONNELLE",
            "DEMISSION", "PRISE_ACTE", "RESILIATION_JUDICIAIRE",
            "LICENCIEMENT_ORDINAIRE", "RUPTURE_AMIABLE"
    );

    private CompensationCalculator() {}

    public static Optional<CompensationEstimate> calculate(
            String typeRupture, Integer annees, Integer mois, Double salaire) {

        if (typeRupture == null) return Optional.empty();
        String normalized = typeRupture.toUpperCase();
        if (!EXTENDED_TYPES.contains(normalized)) return Optional.empty();

        boolean isMacron = MACRON_TYPES.contains(normalized);

        int safeAnnees = annees != null ? annees : 0;
        int safeMois   = mois != null ? mois : 0;
        double safeSalaire = (salaire != null && salaire > 0) ? salaire : 0;

        if (!isMacron) {
            // Type hors Macron (rupture conventionnelle, démission, etc.) :
            // on remonte un estimate partiel pour le pré-remplissage F-DT-09
            // et la cohérence F-IA-03. Aucun chiffre d'indemnité Macron.
            return Optional.of(new CompensationEstimate(
                    0.0, safeSalaire, safeAnnees, safeMois,
                    normalized, 0, 0.0, true));
        }

        boolean donneesPartielles = (annees == null || mois == null
                || salaire == null || salaire <= 0);

        if (annees == null && salaire == null) return Optional.empty();

        double totalYears = safeAnnees + safeMois / 12.0;
        double indemnite  = 0;
        if (totalYears >= 1 && safeSalaire > 0) {
            double years10     = Math.min(totalYears, 10);
            double yearsAbove10 = Math.max(0, totalYears - 10);
            indemnite = safeSalaire * (years10 * 0.25 + yearsAbove10 / 3.0);
        }

        int plafondMin     = macronMin(safeAnnees);
        double plafondMax  = macronMax(safeAnnees);

        double rounded = Math.round(indemnite * 100.0) / 100.0;
        return Optional.of(new CompensationEstimate(
                rounded, safeSalaire, safeAnnees, safeMois,
                normalized, plafondMin, plafondMax, donneesPartielles));
    }

    static int macronMin(int annees) {
        if (annees < 1) return 0;
        if (annees < 2) return 1;
        return 3;
    }

    static double macronMax(int annees) {
        if (annees < 1)   return 1;
        if (annees == 1)  return 2;
        if (annees == 2)  return 3.5;
        if (annees <= 7)  return annees + 1; // 3→4, 4→5, 5→6, 6→7, 7→8
        if (annees == 8)  return 8;          // 8→8 (même max que 7)
        if (annees <= 10) return annees;     // 9→9, 10→10
        double max = 10 + (annees - 10) * 0.5;
        return Math.min(max, 20);
    }
}
