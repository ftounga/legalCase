package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.InaptitudeCalculator.OrigineInaptitude;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InaptitudeCalculatorTest {

    @Test
    void compute_FRPro_8ansReclassementOK_returnsDoubledIndemniteAndPreavis_noDamages() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2500.00"), 8, OrigineInaptitude.PROFESSIONNELLE,
                true, LocalDate.of(2026, 3, 15), "FRANCE");

        // Indemnité légale R.1234-2 : 2500/4 × 8 = 625 × 8 = 5000, puis × 2 = 10000
        assertThat(r.indemniteLegale()).isEqualByComparingTo("10000.00");
        // Préavis 2 × 2500 = 5000
        assertThat(r.indemniteCompensatricePreavis()).isEqualByComparingTo("5000.00");
        // Reclassement respecté → pas de damages
        assertThat(r.damagesReclassement()).isEqualByComparingTo("0.00");
        assertThat(r.total()).isEqualByComparingTo("15000.00");
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.origineInaptitude()).isEqualTo(OrigineInaptitude.PROFESSIONNELLE);
        assertThat(r.baseJuridique()).contains("L.1226-10").contains("L.1226-14");
        assertThat(r.avisMedecinTravailDate()).isEqualTo(LocalDate.of(2026, 3, 15));
    }

    @Test
    void compute_FRNonPro_8ans_returnsSimpleIndemniteNoPreavisNoDamages() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2500.00"), 8, OrigineInaptitude.NON_PROFESSIONNELLE,
                true, null, "FRANCE");

        // Indemnité légale simple : 2500/4 × 8 = 5000 (pas de doublement)
        assertThat(r.indemniteLegale()).isEqualByComparingTo("5000.00");
        assertThat(r.indemniteCompensatricePreavis()).isEqualByComparingTo("0.00");
        assertThat(r.damagesReclassement()).isEqualByComparingTo("0.00");
        assertThat(r.total()).isEqualByComparingTo("5000.00");
        assertThat(r.baseJuridique()).contains("L.1226-2");
    }

    @Test
    void compute_FRPro_reclassementNonRespecte_addsDamages12Months() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2500.00"), 5, OrigineInaptitude.PROFESSIONNELLE,
                false, null, "FRANCE");

        // Indemnité : 2500/4 × 5 × 2 = 6250
        assertThat(r.indemniteLegale()).isEqualByComparingTo("6250.00");
        // Préavis : 2 × 2500 = 5000
        assertThat(r.indemniteCompensatricePreavis()).isEqualByComparingTo("5000.00");
        // Damages : 12 × 2500 = 30000
        assertThat(r.damagesReclassement()).isEqualByComparingTo("30000.00");
        assertThat(r.total()).isEqualByComparingTo("41250.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("L.1226-15"));
    }

    @Test
    void compute_FRNonPro_reclassementNonRespecte_noDamagesL1226_15() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2500.00"), 5, OrigineInaptitude.NON_PROFESSIONNELLE,
                false, null, "FRANCE");
        // Pas de damages L.1226-15 car non-pro
        assertThat(r.damagesReclassement()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteCompensatricePreavis()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_FRPro_ancienneteAu12ans_usesPivot10ansInR1234_2() {
        // 10 premières années × ¼ + 2 ans × ⅓ :
        // (2400/4)×10 + (2400/3)×2 = 6000 + 1600 = 7600, × 2 = 15200
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2400.00"), 12, OrigineInaptitude.PROFESSIONNELLE,
                true, null, "FRANCE");
        assertThat(r.indemniteLegale()).isEqualByComparingTo("15200.00");
    }

    @Test
    void compute_FRPro_ancienneteZeroAn_returnsZeroIndemnite_butStillPreavis() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2500.00"), 0, OrigineInaptitude.PROFESSIONNELLE,
                true, null, "FRANCE");
        // Seuil 1 an R.1234-2 → indemnité = 0
        assertThat(r.indemniteLegale()).isEqualByComparingTo("0.00");
        // Préavis toujours indemnisé pour origine pro
        assertThat(r.indemniteCompensatricePreavis()).isEqualByComparingTo("5000.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("Ancienneté inférieure à 1 an"));
    }

    @Test
    void compute_BEPro_reclassementRespecte_returnsAllZero() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("3000.00"), 10, OrigineInaptitude.PROFESSIONNELLE_BE,
                true, null, "BELGIQUE");
        // Force majeure médicale BE : pas d'indemnité forfaitaire
        assertThat(r.indemniteLegale()).isEqualByComparingTo("0.00");
        assertThat(r.indemniteCompensatricePreavis()).isEqualByComparingTo("0.00");
        assertThat(r.damagesReclassement()).isEqualByComparingTo("0.00");
        assertThat(r.total()).isEqualByComparingTo("0.00");
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.baseJuridique()).contains("Art. 34 Loi 03/07/1978");
    }

    @Test
    void compute_BEPro_reclassementNonRespecte_returnsDamages3Months() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("3000.00"), 10, OrigineInaptitude.PROFESSIONNELLE_BE,
                false, null, "BELGIQUE");
        assertThat(r.indemniteLegale()).isEqualByComparingTo("0.00");
        assertThat(r.damagesReclassement()).isEqualByComparingTo("9000.00");
        assertThat(r.total()).isEqualByComparingTo("9000.00");
        assertThat(r.messages()).anyMatch(m -> m.contains("Trajet de réintégration non respecté"));
    }

    @Test
    void compute_BENonPro_reclassementNonRespecte_returnsDamages3Months() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2000.00"), 5, OrigineInaptitude.NON_PROFESSIONNELLE_BE,
                false, null, "BELGIQUE");
        assertThat(r.damagesReclassement()).isEqualByComparingTo("6000.00");
        assertThat(r.total()).isEqualByComparingTo("6000.00");
        assertThat(r.baseJuridique()).contains("AR 28/05/2003");
    }

    @Test
    void compute_origineFR_onBelgiumWorkspace_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                new BigDecimal("2500"), 5, OrigineInaptitude.PROFESSIONNELLE,
                true, null, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void compute_origineBE_onFranceWorkspace_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                new BigDecimal("2500"), 5, OrigineInaptitude.PROFESSIONNELLE_BE,
                true, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void compute_salaireZero_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                BigDecimal.ZERO, 5, OrigineInaptitude.PROFESSIONNELLE, true, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_salaireNegatif_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                new BigDecimal("-100"), 5, OrigineInaptitude.PROFESSIONNELLE, true, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_ancienneteNegative_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                new BigDecimal("2500"), -1, OrigineInaptitude.PROFESSIONNELLE, true, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ancienneté");
    }

    @Test
    void compute_origineNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                new BigDecimal("2500"), 5, null, true, null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origine");
    }

    @Test
    void compute_countryNull_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                new BigDecimal("2500"), 5, OrigineInaptitude.PROFESSIONNELLE, true, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_countryUnsupported_throwsIllegalArgument() {
        assertThatThrownBy(() -> InaptitudeCalculator.compute(
                new BigDecimal("2500"), 5, OrigineInaptitude.PROFESSIONNELLE, true, null, "LUXEMBOURG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non supporté");
    }

    @ParameterizedTest
    @EnumSource(value = OrigineInaptitude.class, names = {"PROFESSIONNELLE", "NON_PROFESSIONNELLE"})
    void compute_everyOrigineFR_acceptedOnFrance(OrigineInaptitude origine) {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2000.00"), 3, origine, true, null, "FRANCE");
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.origineInaptitude()).isEqualTo(origine);
    }

    @ParameterizedTest
    @EnumSource(value = OrigineInaptitude.class, names = {"PROFESSIONNELLE_BE", "NON_PROFESSIONNELLE_BE"})
    void compute_everyOrigineBE_acceptedOnBelgium(OrigineInaptitude origine) {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2000.00"), 3, origine, true, null, "BELGIQUE");
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.origineInaptitude()).isEqualTo(origine);
    }

    @Test
    void compute_FRPro_avisDate_includedInMessages() {
        LocalDate avis = LocalDate.of(2026, 1, 10);
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2500.00"), 5, OrigineInaptitude.PROFESSIONNELLE,
                true, avis, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("2026-01-10")
                && m.contains("2026-02-10"));
    }

    @Test
    void compute_formule_containsAllComponents_whenFRProUnrespected() {
        InaptitudeResult r = InaptitudeCalculator.compute(
                new BigDecimal("2500.00"), 5, OrigineInaptitude.PROFESSIONNELLE,
                false, null, "FRANCE");
        assertThat(r.formule())
                .contains("R.1234-2")
                .contains("doublement L.1226-14")
                .contains("préavis")
                .contains("L.1226-15")
                .contains("Total");
    }
}
