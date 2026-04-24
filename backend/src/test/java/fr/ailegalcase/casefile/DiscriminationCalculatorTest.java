package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DiscriminationCalculatorTest {

    @Test
    void compute_fr_sexeGrossesse_licenciement_returnsFullRange() {
        DiscriminationResult r = DiscriminationCalculator.compute(
                new BigDecimal("3000.00"), "SEXE_GROSSESSE", "LICENCIEMENT", "FRANCE");
        assertThat(r.fourchetteMin()).isEqualByComparingTo("18000.00"); // 6 × 3000
        assertThat(r.fourchetteMax()).isEqualByComparingTo("36000.00"); // 12 × 3000
        assertThat(r.fourchetteMediane()).isEqualByComparingTo("27000.00");
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.baseJuridique()).contains("L.1134-5").contains("grossesse");
    }

    @Test
    void compute_fr_age_differenceSalariale_returnsModulatedRange() {
        // AGE base = [3, 8], DIFFERENCE_SALARIALE modulation = [0.4, 0.7]
        // → min = 3 × 0.4 = 1.2, max = 8 × 0.7 = 5.6
        DiscriminationResult r = DiscriminationCalculator.compute(
                new BigDecimal("2000.00"), "AGE", "DIFFERENCE_SALARIALE", "FRANCE");
        assertThat(r.fourchetteMin()).isEqualByComparingTo("2400.00");  // 2000 × 1.2
        assertThat(r.fourchetteMax()).isEqualByComparingTo("11200.00"); // 2000 × 5.6
    }

    @Test
    void compute_be_racial_licenciement_returnsFullRange() {
        DiscriminationResult r = DiscriminationCalculator.compute(
                new BigDecimal("2500.00"), "DISCRIMINATION_RACIALE_BE", "LICENCIEMENT", "BELGIQUE");
        assertThat(r.fourchetteMin()).isEqualByComparingTo("15000.00"); // 6 × 2500
        assertThat(r.fourchetteMax()).isEqualByComparingTo("30000.00"); // 12 × 2500
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.baseJuridique()).contains("Loi 30/7/1981").contains("Loi 10/5/2007");
    }

    @Test
    void compute_motifFr_workspaceBe_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                new BigDecimal("3000"), "SEXE_GROSSESSE", "LICENCIEMENT", "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incompatible");
    }

    @Test
    void compute_motifBe_workspaceFr_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                new BigDecimal("3000"), "DISCRIMINATION_RACIALE_BE", "LICENCIEMENT", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incompatible");
    }

    @Test
    void compute_unknownMotif_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                new BigDecimal("3000"), "RELIGION_XYZ", "LICENCIEMENT", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inconnu");
    }

    @Test
    void compute_unknownContexte_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                new BigDecimal("3000"), "SEXE_GROSSESSE", "AUTRE", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Contexte");
    }

    @Test
    void compute_unknownCountry_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                new BigDecimal("3000"), "SEXE_GROSSESSE", "LICENCIEMENT", "ALLEMAGNE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullSalaire_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                null, "SEXE_GROSSESSE", "LICENCIEMENT", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_negativeSalaire_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                new BigDecimal("-1"), "SEXE_GROSSESSE", "LICENCIEMENT", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullMotif_throws() {
        assertThatThrownBy(() -> DiscriminationCalculator.compute(
                new BigDecimal("3000"), null, "LICENCIEMENT", "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_allFrMotifsAccepted() {
        for (String motif : DiscriminationCalculator.getFrMotifs()) {
            DiscriminationResult r = DiscriminationCalculator.compute(
                    new BigDecimal("3000"), motif, "LICENCIEMENT", "FRANCE");
            assertThat(r.motifDiscrimination()).isEqualTo(motif);
            assertThat(r.fourchetteMin().signum()).isPositive();
            assertThat(r.fourchetteMax().signum()).isPositive();
        }
    }

    @Test
    void compute_allBeMotifsAccepted() {
        for (String motif : DiscriminationCalculator.getBeMotifs()) {
            DiscriminationResult r = DiscriminationCalculator.compute(
                    new BigDecimal("3000"), motif, "LICENCIEMENT", "BELGIQUE");
            assertThat(r.motifDiscrimination()).isEqualTo(motif);
            assertThat(r.fourchetteMin().signum()).isPositive();
        }
    }

    @Test
    void compute_allContextesAccepted() {
        for (String ctx : DiscriminationCalculator.getContextes()) {
            DiscriminationResult r = DiscriminationCalculator.compute(
                    new BigDecimal("3000"), "SEXE_GROSSESSE", ctx, "FRANCE");
            assertThat(r.contexteActe()).isEqualTo(ctx);
        }
    }

    @Test
    void compute_messages_fr_containsProbatoire() {
        DiscriminationResult r = DiscriminationCalculator.compute(
                new BigDecimal("3000"), "SEXE_GROSSESSE", "LICENCIEMENT", "FRANCE");
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages().get(0)).contains("L.1134-1").contains("prouver");
    }

    @Test
    void compute_messages_be_containsLoi2007() {
        DiscriminationResult r = DiscriminationCalculator.compute(
                new BigDecimal("3000"), "DISCRIMINATION_GENRE_BE", "LICENCIEMENT", "BELGIQUE");
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages().get(0)).contains("10/5/2007").contains("discrimination");
    }
}
