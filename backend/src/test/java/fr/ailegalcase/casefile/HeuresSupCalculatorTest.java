package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HeuresSupCalculatorTest {

    // ---- FR ----

    @Test
    void compute_fr_40h25_15euro_returns150() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 0, null, null, null, null);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "FRANCE");

        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.rappelMajoration25pct()).isEqualByComparingTo("150.00");
        assertThat(r.rappelMajoration50pct()).isEqualByComparingTo("0.00");
        assertThat(r.rappelTotal()).isEqualByComparingTo("150.00");
        assertThat(r.reposCompensateurHeuresDues()).isEqualByComparingTo("0.00");
        assertThat(r.baseJuridique()).contains("L.3121-28").contains("D.3121-24");
    }

    @Test
    void compute_fr_10h50_15euro_returns75() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 0, 10, 0, null, null, null, null);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "FRANCE");

        assertThat(r.rappelMajoration25pct()).isEqualByComparingTo("0.00");
        assertThat(r.rappelMajoration50pct()).isEqualByComparingTo("75.00");
        assertThat(r.rappelTotal()).isEqualByComparingTo("75.00");
    }

    @Test
    void compute_fr_combined25_50_returnsSum() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 10, 0, null, null, null, null);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "FRANCE");

        assertThat(r.rappelMajoration25pct()).isEqualByComparingTo("150.00");
        assertThat(r.rappelMajoration50pct()).isEqualByComparingTo("75.00");
        assertThat(r.rappelTotal()).isEqualByComparingTo("225.00");
    }

    @Test
    void compute_fr_heuresHorsContingent_setsReposCompensateur() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 12, null, null, null, null);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "FRANCE");

        assertThat(r.reposCompensateurHeuresDues()).isEqualByComparingTo("12.00");
        assertThat(r.heuresHorsContingent()).isEqualTo(12);
    }

    @Test
    void compute_fr_customTauxMajoration25_appliesCcn() {
        // CCN réduit à 10 %
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 0,
                new BigDecimal("10"), null, null, null);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "FRANCE");

        // 40 × 15 × 0.10 = 60
        assertThat(r.rappelMajoration25pct()).isEqualByComparingTo("60.00");
        assertThat(r.tauxMajoration25()).isEqualByComparingTo("10.00");
    }

    @Test
    void compute_fr_messages_containsPrescriptionTriennale() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 0, null, null, null, null);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "FRANCE");

        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages().get(0)).contains("L.3245-1").contains("3 ans");
    }

    @Test
    void compute_fr_rejectsBeField_heuresSupSemaine() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 0, null, null, 10, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresSupSemaine");
    }

    @Test
    void compute_fr_tauxMajoration25_below10_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 0,
                new BigDecimal("5"), null, null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxMajoration25");
    }

    @Test
    void compute_fr_tauxMajoration50_above50_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 0, 10, 0,
                null, new BigDecimal("75"), null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxMajoration50");
    }

    // ---- BE ----

    @Test
    void compute_be_30h50_15euro_returns225() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), null, null, null, null, null, 30, 0);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "BELGIQUE");

        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.rappelMajoration50pct()).isEqualByComparingTo("225.00");
        assertThat(r.rappelMajoration100pct()).isEqualByComparingTo("0.00");
        assertThat(r.rappelTotal()).isEqualByComparingTo("225.00");
        assertThat(r.baseJuridique()).contains("16 mars 1971");
    }

    @Test
    void compute_be_5hDimanche_15euro_returns75() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), null, null, null, null, null, 0, 5);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "BELGIQUE");

        assertThat(r.rappelMajoration100pct()).isEqualByComparingTo("75.00");
        assertThat(r.rappelTotal()).isEqualByComparingTo("75.00");
    }

    @Test
    void compute_be_reposCompensatoire_sumsHours() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), null, null, null, null, null, 30, 5);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "BELGIQUE");

        assertThat(r.reposCompensateurHeuresDues()).isEqualByComparingTo("35.00");
    }

    @Test
    void compute_be_messages_containsPrescriptionQuinquennale() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), null, null, null, null, null, 30, 0);
        HeuresSupResult r = HeuresSupCalculator.compute(req, "BELGIQUE");

        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages().get(0)).contains("2262bis");
    }

    @Test
    void compute_be_rejectsFrField_heuresSupDeclarees25pct() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 0, null, null, 10, 0);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresSupDeclarees25pct");
    }

    @Test
    void compute_be_rejectsFrField_heuresHorsContingent() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), null, null, 5, null, null, 10, 0);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("heuresHorsContingent");
    }

    // ---- Validations communes ----

    @Test
    void compute_tauxHoraireBrut_zero_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                BigDecimal.ZERO, 40, 0, 0, null, null, null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Taux horaire");
    }

    @Test
    void compute_tauxHoraireBrut_negative_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("-1"), 40, 0, 0, null, null, null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_tauxHoraireBrut_null_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                null, 40, 0, 0, null, null, null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_fr_allHoursZero_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 0, 0, 0, null, null, null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Au moins une heure");
    }

    @Test
    void compute_be_allHoursZero_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), null, null, null, null, null, 0, 0);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Au moins une heure");
    }

    @Test
    void compute_negativeHours_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), -1, 0, 0, null, null, null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    @Test
    void compute_unknownCountry_throws() {
        HeuresSupRequest req = new HeuresSupRequest(
                new BigDecimal("15.00"), 40, 0, 0, null, null, null, null);
        assertThatThrownBy(() -> HeuresSupCalculator.compute(req, "ALLEMAGNE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country");
    }

    @Test
    void compute_nullRequest_throws() {
        assertThatThrownBy(() -> HeuresSupCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
