package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndemniteFinMissionInterimCalculatorTest {

    @Test
    void compute_nominal_returnsTenPct() {
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12000.00"), 90, null, LocalDate.of(2026, 4, 25));
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("1200.00");
        assertThat(r.tauxApplique()).isEqualByComparingTo("0.10");
        assertThat(r.exclusionRetenue()).isFalse();
        assertThat(r.motifExclusion()).isNull();
        assertThat(r.dureeMissionJours()).isEqualTo(90);
        assertThat(r.dateFinMission()).isEqualTo(LocalDate.of(2026, 4, 25));
        assertThat(r.baseJuridique()).isEqualTo("Art. L.1251-32 Code du travail");
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.formule()).contains("10 %").contains("1 200,00");
    }

    @Test
    void compute_smallAmount_rounding() {
        // 1000.00 * 10 / 100 = 100.00
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("1000.00"), 30, null, null);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("100.00");
    }

    @Test
    void compute_amountWithRounding_halfUp() {
        // 12347.89 * 10 / 100 = 1234.789 -> arrondi 1234.79 (HALF_UP)
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12347.89"), 60, null, null);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("1234.79");
    }

    @Test
    void compute_largeAmount_returnsCorrect() {
        // 250 000 € sur 18 mois — 25 000 €
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("250000.00"), 540, null, null);
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("25000.00");
        assertThat(r.formule()).contains("250 000,00").contains("25 000,00");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CONTRAT_INDETERMINEE_PROPOSE",
            "RUPTURE_ANTICIPEE_SALARIE",
            "FAUTE_GRAVE",
            "FORCE_MAJEURE",
            "MISSION_PEPINIERE_QUALIFIANTE",
            "INTERIMAIRE_REFUS_PROPOSITION_CDI"
    })
    void compute_allSixExclusions_returnZero(String motif) {
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12000.00"), 90, motif, LocalDate.of(2026, 4, 25));
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("0.00");
        assertThat(r.exclusionRetenue()).isTrue();
        assertThat(r.motifExclusion()).isEqualTo(motif);
        assertThat(r.baseJuridique()).contains("L.1251-32");
        assertThat(r.messages()).hasSize(1);
        assertThat(r.messages().get(0)).containsAnyOf("L.1251-32", "L.1251-33");
        assertThat(r.formule()).isEqualTo("0,00 €");
        // Le taux légal reste exposé même en cas d'exclusion (info pour le frontend).
        assertThat(r.tauxApplique()).isEqualByComparingTo("0.10");
    }

    @Test
    void compute_negativeAmount_throws() {
        assertThatThrownBy(() -> IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("-500.00"), 90, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_zeroAmount_throws() {
        assertThatThrownBy(() -> IndemniteFinMissionInterimCalculator.compute(
                BigDecimal.ZERO, 90, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullAmount_throws() {
        assertThatThrownBy(() -> IndemniteFinMissionInterimCalculator.compute(
                null, 90, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_zeroDuree_throws() {
        assertThatThrownBy(() -> IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12000.00"), 0, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Durée");
    }

    @Test
    void compute_negativeDuree_throws() {
        assertThatThrownBy(() -> IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12000.00"), -1, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Durée");
    }

    @Test
    void compute_unknownExclusion_throws() {
        assertThatThrownBy(() -> IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12000.00"), 90, "MOTIF_INVENTE", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MOTIF_INVENTE");
    }

    @Test
    void compute_messageContainsBaseLegale() {
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("18500.00"), 120, null, null);
        assertThat(r.messages()).isNotEmpty();
        assertThat(r.messages().stream().anyMatch(m -> m.contains("L.1251-32"))).isTrue();
    }

    @Test
    void compute_nullDateFinMission_isAllowed() {
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12000.00"), 90, null, null);
        assertThat(r.dateFinMission()).isNull();
        assertThat(r.montantIndemniteEur()).isEqualByComparingTo("1200.00");
    }

    @Test
    void compute_taux_alwaysTenPctNeverSixPct() {
        // Différence vs F-DT-17 : pas de taux 6 % à l'intérim.
        IndemniteFinMissionInterimResult r = IndemniteFinMissionInterimCalculator.compute(
                new BigDecimal("12000.00"), 90, null, null);
        assertThat(r.tauxApplique()).isEqualByComparingTo("0.10");
        // Le message explicite cette différence métier.
        assertThat(r.messages().stream().anyMatch(m -> m.contains("L.1243-9") || m.contains("CCN"))).isTrue();
    }
}
