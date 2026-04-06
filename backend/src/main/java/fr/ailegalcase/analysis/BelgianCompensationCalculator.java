package fr.ailegalcase.analysis;

import java.util.Optional;

/**
 * Calcule l'indemnité compensatoire de préavis belge et la fourchette CCT 109.
 *
 * Table de préavis : Loi du 26 décembre 2013 (art. 37/2 Loi du 3 juillet 1978).
 * CCT 109 : indemnité pour licenciement manifestement déraisonnable (3 à 17 semaines).
 *
 * Indemnité compensatoire = salaire hebdomadaire brut × nombre de semaines de préavis.
 * Salaire hebdomadaire = salaire mensuel brut × 12 / 52.
 */
public final class BelgianCompensationCalculator {

    public record BelgianCompensationEstimate(
            int preavisSemaines,
            double indemniteCompensatoire,
            double salaireHebdomadaire,
            double salaireReference,
            int ancienneteAnnees,
            int ancienneteMois,
            double cct109MinSemaines,
            double cct109MaxSemaines,
            double cct109MinEuros,
            double cct109MaxEuros,
            boolean donneesPartielles
    ) {}

    private BelgianCompensationCalculator() {}

    public static Optional<BelgianCompensationEstimate> calculate(
            Integer annees, Integer mois, Double salaireMensuel) {

        if (annees == null && salaireMensuel == null) return Optional.empty();

        boolean donneesPartielles = (annees == null || mois == null
                || salaireMensuel == null || salaireMensuel <= 0);

        int safeAnnees = annees != null ? annees : 0;
        int safeMois = mois != null ? mois : 0;
        double safeSalaire = (salaireMensuel != null && salaireMensuel > 0) ? salaireMensuel : 0;

        int totalMois = safeAnnees * 12 + safeMois;
        int preavis = calculerPreavisSemaines(totalMois);
        double salaireHebdo = safeSalaire * 12.0 / 52.0;
        double indemnite = Math.round(salaireHebdo * preavis * 100.0) / 100.0;

        double cct109Min = Math.round(salaireHebdo * 3 * 100.0) / 100.0;
        double cct109Max = Math.round(salaireHebdo * 17 * 100.0) / 100.0;

        return Optional.of(new BelgianCompensationEstimate(
                preavis, indemnite, Math.round(salaireHebdo * 100.0) / 100.0,
                safeSalaire, safeAnnees, safeMois,
                3, 17, cct109Min, cct109Max, donneesPartielles));
    }

    /**
     * Table de préavis (Loi du 26 décembre 2013, art. 37/2).
     * @param totalMois ancienneté totale en mois
     * @return nombre de semaines de préavis
     */
    static int calculerPreavisSemaines(int totalMois) {
        if (totalMois < 3) return 1;
        if (totalMois < 6) return 3;
        if (totalMois < 9) return 4;
        if (totalMois < 12) return 5;
        if (totalMois < 15) return 6;
        if (totalMois < 18) return 7;
        if (totalMois < 21) return 8;
        if (totalMois < 24) return 9;

        int annees = totalMois / 12;
        if (annees < 3) return 10;
        if (annees < 4) return 12;
        if (annees < 5) return 13;

        // 5 à 19 ans : 15 + 3 par année au-delà de 5
        if (annees < 20) return 15 + (annees - 5) * 3;

        // 20 ans = 62 semaines (palier officiel)
        // > 20 ans : 62 + 1 par année au-delà de 20
        return 62 + (annees - 20);
    }
}
