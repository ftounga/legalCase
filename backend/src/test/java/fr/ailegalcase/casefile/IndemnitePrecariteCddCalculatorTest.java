package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndemnitePrecariteCddCalculatorTest {

    @Test
    void compute_nominal_10pct_returnsCorrectAmount() {
        IndemnitePrecariteCddResult r = IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("18500.00"), 10, null);
        assertThat(r.indemnitePrecarite()).isEqualByComparingTo("1850.00");
        assertThat(r.tauxPrecarite()).isEqualTo(10);
        assertThat(r.casExclusion()).isNull();
        assertThat(r.formule()).contains("10 %").contains("1850,00");
        assertThat(r.baseJuridique()).isEqualTo("Art. L.1243-8 Code du travail");
        assertThat(r.messages()).isNotEmpty();
    }

    @Test
    void compute_nominal_6pct_returnsCorrectAmount_withReducedRateMessage() {
        IndemnitePrecariteCddResult r = IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("18500.00"), 6, null);
        assertThat(r.indemnitePrecarite()).isEqualByComparingTo("1110.00");
        assertThat(r.tauxPrecarite()).isEqualTo(6);
        assertThat(r.messages()).anyMatch(m -> m.contains("L.1243-9"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CDD_ETUDIANT_VACANCES",
            "CDD_SAISONNIER",
            "CDD_USAGE",
            "CDI_REFUSE_PAR_SALARIE",
            "RUPTURE_ANTICIPEE_SALARIE",
            "RUPTURE_ANTICIPEE_FAUTE_GRAVE"
    })
    void compute_allSixExclusions_returnZeroWithArticleCitation(String exclusion) {
        IndemnitePrecariteCddResult r = IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("18500.00"), 10, exclusion);
        assertThat(r.indemnitePrecarite()).isEqualByComparingTo("0.00");
        assertThat(r.casExclusion()).isEqualTo(exclusion);
        assertThat(r.baseJuridique()).contains("L.1243-10");
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages().get(0)).contains("L.1243-10");
    }

    @Test
    void compute_negativeSalaires_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("-500.00"), 10, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_zeroSalaires_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePrecariteCddCalculator.compute(
                BigDecimal.ZERO, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullSalaires_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePrecariteCddCalculator.compute(null, 10, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_invalidRate_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("18500.00"), 15, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 %").hasMessageContaining("6 %");
    }

    @Test
    void compute_unknownExclusionCode_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("18500.00"), 10, "AUTRE_MOTIF_INVENTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("AUTRE_MOTIF_INVENTE");
    }

    @Test
    void compute_formula_rounding_at2Decimals() {
        // 12347.89 * 10 / 100 = 1234.789 -> arrondi 1234.79 (HALF_UP)
        IndemnitePrecariteCddResult r = IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("12347.89"), 10, null);
        assertThat(r.indemnitePrecarite()).isEqualByComparingTo("1234.79");
    }

    @Test
    void compute_smallAmount_atSixPct_rounding() {
        // 1000 * 6 / 100 = 60.00
        IndemnitePrecariteCddResult r = IndemnitePrecariteCddCalculator.compute(
                new BigDecimal("1000.00"), 6, null);
        assertThat(r.indemnitePrecarite()).isEqualByComparingTo("60.00");
    }
}
