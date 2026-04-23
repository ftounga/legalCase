package fr.ailegalcase.analysis;

import java.util.Optional;

/**
 * Calcule la prestation compensatoire indicative (Art. 271 Code civil).
 *
 * La prestation compensatoire est fixée discrétionnairement par le juge —
 * ce calcul est purement indicatif et ne constitue pas un avis juridique.
 *
 * Formule :
 *   base mensuelle = |revenus_A - revenus_B| × coeff
 *   montant capitalisé = base × 12 × DUREE_REFERENCE_ANS
 *
 * Coefficients : France 0.30 / Belgique 0.25
 * Durée de référence conventionnelle : 8 ans
 * Fourchette ±15 % (montantMin / montantMax)
 */
public final class PrestationCompensatoireCalculator {

    public record PrestationCompensatoireEstimate(
            double montantMin,
            double montantMax,
            double ecartRevenus,
            int dureeMarriage,
            String pays,
            boolean donneesPartielles,
            JurisprudenceRange jurisprudenceRange
    ) {
        /** Rétrocompat sans fourchette JAF (pré-F-153). */
        public PrestationCompensatoireEstimate(double montantMin, double montantMax,
                                                double ecartRevenus, int dureeMarriage,
                                                String pays, boolean donneesPartielles) {
            this(montantMin, montantMax, ecartRevenus, dureeMarriage, pays, donneesPartielles, null);
        }
    }

    private static final double COEFF_FRANCE   = 0.30;
    private static final double COEFF_BELGIQUE = 0.25;
    private static final int    DUREE_REFERENCE_ANS = 8;

    private PrestationCompensatoireCalculator() {}

    public static Optional<PrestationCompensatoireEstimate> calculate(
            Double revenusA, Double revenusB, Integer dureeMarriage, String pays) {

        if (revenusA == null && revenusB == null && dureeMarriage == null) {
            return Optional.empty();
        }

        boolean donneesPartielles = (revenusA == null || revenusB == null || dureeMarriage == null);

        double safeA      = revenusA    != null ? revenusA    : 0;
        double safeB      = revenusB    != null ? revenusB    : 0;
        int    safeDuree  = dureeMarriage != null ? dureeMarriage : 0;
        String safePays   = "BELGIQUE".equalsIgnoreCase(pays) ? "BELGIQUE" : "FRANCE";
        double coeff      = "BELGIQUE".equals(safePays) ? COEFF_BELGIQUE : COEFF_FRANCE;

        double ecart   = Math.abs(safeA - safeB);
        double montant = Math.round(ecart * coeff * 12 * DUREE_REFERENCE_ANS * 100.0) / 100.0;

        double montantMin = Math.round(montant * 0.85 * 100.0) / 100.0;
        double montantMax = Math.round(montant * 1.15 * 100.0) / 100.0;

        // F-153 SF-153-01 : fourchette jurisprudentielle (dispersion ± 50 %
        // car art. 271 Cciv laisse le JAF entièrement libre).
        JurisprudenceRange jafRange = donneesPartielles
                ? null
                : JafReferenceTable.prestationCompensatoireRange(montant, safePays);

        return Optional.of(new PrestationCompensatoireEstimate(
                montantMin,
                montantMax,
                Math.round(ecart * 100.0) / 100.0,
                safeDuree,
                safePays,
                donneesPartielles,
                jafRange
        ));
    }
}
