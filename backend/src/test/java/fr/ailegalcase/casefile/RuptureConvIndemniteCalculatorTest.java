package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuptureConvIndemniteCalculatorTest {

    @Test
    void zeroAn_returnsZero() {
        RuptureConvIndemniteResult r = RuptureConvIndemniteCalculator.computeMinimumLegal(
                0, new BigDecimal("2979"));

        assertThat(r.indemniteLegaleMinimum()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.formule()).contains("non due");
        assertThat(r.baseJuridique()).isEqualTo("Art. R1234-2 Code du travail");
    }

    @Test
    void dossierE28_4ans_2979Euros_returns2979() {
        // Réplique du cas E28 observé en staging 2026-04-20
        RuptureConvIndemniteResult r = RuptureConvIndemniteCalculator.computeMinimumLegal(
                4, new BigDecimal("2979"));

        assertThat(r.indemniteLegaleMinimum()).isEqualByComparingTo(new BigDecimal("2979.00"));
        assertThat(r.formule()).contains("¼").contains("4").contains("2979");
    }

    @Test
    void dixAnsPile_returnsQuartMoisTimesDix() {
        // 10 × ¼ × 3000 = 7500
        RuptureConvIndemniteResult r = RuptureConvIndemniteCalculator.computeMinimumLegal(
                10, new BigDecimal("3000"));

        assertThat(r.indemniteLegaleMinimum()).isEqualByComparingTo(new BigDecimal("7500.00"));
    }

    @Test
    void quinzeAns_cumuleLesDeuxTranches() {
        // Partie 1 : 10 × ¼ × 3000 = 7500
        // Partie 2 : 5 × ⅓ × 3000 = 5000
        // Total : 12 500
        RuptureConvIndemniteResult r = RuptureConvIndemniteCalculator.computeMinimumLegal(
                15, new BigDecimal("3000"));

        assertThat(r.indemniteLegaleMinimum()).isEqualByComparingTo(new BigDecimal("12500.00"));
        assertThat(r.formule()).contains("¼").contains("⅓");
    }

    @Test
    void salaireNull_raiseIllegalArgument() {
        assertThatThrownBy(() ->
                RuptureConvIndemniteCalculator.computeMinimumLegal(5, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaireMensuel requis");
    }

    @Test
    void salaireNegatif_returnsZero() {
        RuptureConvIndemniteResult r = RuptureConvIndemniteCalculator.computeMinimumLegal(
                5, new BigDecimal("-100"));

        assertThat(r.indemniteLegaleMinimum()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.formule()).contains("non due");
    }

    @Test
    void salaireZero_returnsZero() {
        RuptureConvIndemniteResult r = RuptureConvIndemniteCalculator.computeMinimumLegal(
                5, BigDecimal.ZERO);

        assertThat(r.indemniteLegaleMinimum()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void messages_contient_L1237_13_et_conventionCollective() {
        RuptureConvIndemniteResult r = RuptureConvIndemniteCalculator.computeMinimumLegal(
                3, new BigDecimal("2500"));

        assertThat(r.messages()).anyMatch(m -> m.contains("L1237-13"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("convention collective"));
    }
}
