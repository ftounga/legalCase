package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndemniteTravailDissimuleCalculatorTest {

    @Test
    void compute_nominal_returns6moisSalaire() {
        TravailDissimuleResult r = IndemniteTravailDissimuleCalculator.compute(new BigDecimal("2500.00"));
        assertThat(r.indemniteForfaitaire()).isEqualByComparingTo("15000.00");
        assertThat(r.salaireMensuelReference()).isEqualByComparingTo("2500.00");
        assertThat(r.baseJuridique()).isEqualTo("Art. L.8223-1 Code du travail");
    }

    @Test
    void compute_arbitraryAmount_multiplesBy6() {
        TravailDissimuleResult r = IndemniteTravailDissimuleCalculator.compute(new BigDecimal("3450.75"));
        assertThat(r.indemniteForfaitaire()).isEqualByComparingTo("20704.50");
    }

    @Test
    void compute_negativeSalaire_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemniteTravailDissimuleCalculator.compute(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_zeroSalaire_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemniteTravailDissimuleCalculator.compute(BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullSalaire_throwsIllegalArgument() {
        assertThatThrownBy(() -> IndemniteTravailDissimuleCalculator.compute(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_formula_properlyFormatted() {
        TravailDissimuleResult r = IndemniteTravailDissimuleCalculator.compute(new BigDecimal("2500.00"));
        assertThat(r.formule())
                .contains("6 mois")
                .contains("2500,00")
                .contains("15000,00");
    }

    @Test
    void compute_messages_contains3Items_includingCumulAndNonCumul() {
        TravailDissimuleResult r = IndemniteTravailDissimuleCalculator.compute(new BigDecimal("2500.00"));
        assertThat(r.messages()).hasSize(3);
        assertThat(r.messages().get(0)).contains("cumulable").contains("Cass. soc.");
        assertThat(r.messages().get(1)).contains("L.8221-3").contains("L.8221-5");
        assertThat(r.messages().get(2)).contains("Non cumulable").contains("visite médicale");
    }
}
