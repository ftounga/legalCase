package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-217-08 : tests unitaires de {@link ContributionConjointBeCalculator}.
 */
class ContributionConjointBeCalculatorTest {

    private ContributionConjointBeInput input(
            ContributionConjointBeCalculator.TypeDivorce type,
            boolean renonciation, boolean besoin, boolean fauteGrave,
            int dureeMariage, String revenuCreancier, String revenuDebiteur,
            boolean degradation) {
        return new ContributionConjointBeInput(
                type, renonciation, besoin, fauteGrave, dureeMariage,
                new BigDecimal(revenuCreancier), new BigDecimal(revenuDebiteur),
                degradation, null);
    }

    @Test
    void compute_divorceDc_pensionConventionnelle() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DC,
                        false, true, false, 10, "900", "3000", false),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionConjointBeCalculator.Verdict.PENSION_CONVENTIONNELLE);
        assertThat(r.dureeMaximaleMois()).isZero();
        assertThat(r.montantMensuelIndicatif()).isEqualByComparingTo("0");
    }

    @Test
    void compute_divorceDdi_besoin_sansFaute_pensionDue() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, 18, "900", "3600", true),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionConjointBeCalculator.Verdict.PENSION_DUE);
        // Durée = 18 ans × 12.
        assertThat(r.dureeMaximaleMois()).isEqualTo(216);
        // Besoin = 3600 - 900 = 2700 ; plafond tiers = 1200 → montant = 1200.
        assertThat(r.montantMensuelIndicatif()).isEqualByComparingTo("1200.00");
        assertThat(r.plafondTiersRevenusDebiteur()).isEqualByComparingTo("1200.00");
        assertThat(r.country()).isEqualTo("BELGIQUE");
    }

    @Test
    void compute_divorceDdi_fauteGrave_pensionNonDue() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, true, 18, "900", "3600", false),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionConjointBeCalculator.Verdict.PENSION_NON_DUE);
        assertThat(r.motifsExclusion())
                .contains(ContributionConjointBeCalculator.MotifExclusion.FAUTE_GRAVE_CREANCIER);
    }

    @Test
    void compute_divorceDdi_renonciation_pensionNonDue() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        true, true, false, 18, "900", "3600", false),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionConjointBeCalculator.Verdict.PENSION_NON_DUE);
        assertThat(r.motifsExclusion())
                .contains(ContributionConjointBeCalculator.MotifExclusion.RENONCIATION_CONVENTIONNELLE);
    }

    @Test
    void compute_divorceDdi_creancierHorsBesoin_pensionNonDue() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, false, false, 18, "2000", "3600", false),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionConjointBeCalculator.Verdict.PENSION_NON_DUE);
        assertThat(r.motifsExclusion())
                .contains(ContributionConjointBeCalculator.MotifExclusion.CREANCIER_HORS_BESOIN);
    }

    @Test
    void compute_divorceDdi_revenusNuls_donneesInsuffisantes() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, 18, "0", "0", false),
                "BELGIQUE");

        assertThat(r.verdict())
                .isEqualTo(ContributionConjointBeCalculator.Verdict.DONNEES_INSUFFISANTES);
    }

    @Test
    void compute_montantPlafonneAuTiersDesRevenusDuDebiteur() {
        // Écart de revenus 6000 - 0 = 6000 mais plafond tiers = 6000/3 = 2000.
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, 12, "0", "6000", false),
                "BELGIQUE");

        assertThat(r.montantMensuelIndicatif()).isEqualByComparingTo("2000.00");
    }

    @Test
    void compute_dureeMax_egaleAuMariageEnMois() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, 7, "1000", "2500", false),
                "BELGIQUE");

        assertThat(r.dureeMaximaleMois()).isEqualTo(84);
    }

    @Test
    void compute_montantArrondiADeuxDecimales() {
        ContributionConjointBeResult r = ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, 10, "833", "2467", false),
                "BELGIQUE");

        assertThat(r.montantMensuelIndicatif().scale()).isEqualTo(2);
        assertThat(r.plafondTiersRevenusDebiteur().scale()).isEqualTo(2);
    }

    @Test
    void compute_paysFrance_rejette() {
        assertThatThrownBy(() -> ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, 10, "900", "3000", false),
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void compute_typeDivorceManquant_rejette() {
        ContributionConjointBeInput in = new ContributionConjointBeInput(
                null, false, true, false, 10,
                new BigDecimal("900"), new BigDecimal("3000"), false, null);
        assertThatThrownBy(() -> ContributionConjointBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeDivorce");
    }

    @Test
    void compute_dureeMariageNegative_rejette() {
        assertThatThrownBy(() -> ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, -1, "900", "3000", false),
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durée du mariage");
    }

    @Test
    void compute_revenuNegatif_rejette() {
        assertThatThrownBy(() -> ContributionConjointBeCalculator.compute(
                input(ContributionConjointBeCalculator.TypeDivorce.DDI,
                        false, true, false, 10, "-100", "3000", false),
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }
}
