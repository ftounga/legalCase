package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AvantagesConventionnelsBeCalculatorTest {

    private static final BigDecimal SALAIRE_3000 = new BigDecimal("3000.00");

    @Test
    void compute_nominalCp200_full_returns5LinesAndCorrectTotal() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, true, true, true, true, 220);

        // Pécule simple = 36000 × 0.0767 = 2761.20
        assertThat(r.peculeVacancesSimpleEur()).isEqualByComparingTo("2761.20");
        // Double pécule = 36000 × 0.5 × 0.92 = 16560
        assertThat(r.doublePeculeVacancesEur()).isEqualByComparingTo("16560.00");
        // Prime = 3000
        assertThat(r.primeFinAnneeEur()).isEqualByComparingTo("3000.00");
        // Éco-chèques = 250
        assertThat(r.ecoChequesValeurAnnuelleEur()).isEqualByComparingTo("250.00");
        // Chèques-repas = 220 × 8 = 1760
        assertThat(r.chequesRepasValeurAnnuelleEur()).isEqualByComparingTo("1760.00");
        // Total = 2761.20 + 16560 + 3000 + 250 + 1760 = 24331.20
        assertThat(r.totalAvantagesAnnuelsEur()).isEqualByComparingTo("24331.20");
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.baseJuridique()).contains("Loi 28/06/1971").contains("CCT 98").contains("CCT 19octies");
    }

    @Test
    void compute_peculeOnly_otherFalse_returnsOnlyPecules() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, false, false, false, false, 0);

        assertThat(r.peculeVacancesSimpleEur()).isEqualByComparingTo("2761.20");
        assertThat(r.doublePeculeVacancesEur()).isEqualByComparingTo("16560.00");
        assertThat(r.primeFinAnneeEur()).isEqualByComparingTo("0.00");
        assertThat(r.ecoChequesValeurAnnuelleEur()).isEqualByComparingTo("0.00");
        assertThat(r.chequesRepasValeurAnnuelleEur()).isEqualByComparingTo("0.00");
        assertThat(r.totalAvantagesAnnuelsEur()).isEqualByComparingTo("19321.20");
    }

    @Test
    void compute_doublePeculeAlreadyPaid_doublePeculeIsZero() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                true, true, true, true, true, 220);

        assertThat(r.doublePeculeVacancesEur()).isEqualByComparingTo("0.00");
        // Pécule simple toujours dû
        assertThat(r.peculeVacancesSimpleEur()).isEqualByComparingTo("2761.20");
    }

    @Test
    void compute_primeNotPlanned_primeIsZero() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, false, true, true, true, 220);

        assertThat(r.primeFinAnneeEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_ecoChequesNotPlanned_ecoChequesIsZero() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, true, false, false, true, 220);

        assertThat(r.ecoChequesValeurAnnuelleEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_chequesRepasNotPlanned_chequesRepasIsZero() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, true, true, true, false, 220);

        assertThat(r.chequesRepasValeurAnnuelleEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_zeroJoursPrestes_chequesRepasIsZeroEvenIfPlanned() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, true, true, true, true, 0);

        assertThat(r.chequesRepasValeurAnnuelleEur()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_lowSalary_allLinesProportional() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                new BigDecimal("1500.00"), 200, 12, "CP_200", 2026,
                false, true, true, true, true, 200);

        // 1500 × 12 × 0.0767 = 1380.60
        assertThat(r.peculeVacancesSimpleEur()).isEqualByComparingTo("1380.60");
        // 1500 × 12 × 0.5 × 0.92 = 8280
        assertThat(r.doublePeculeVacancesEur()).isEqualByComparingTo("8280.00");
        assertThat(r.primeFinAnneeEur()).isEqualByComparingTo("1500.00");
    }

    @Test
    void compute_zeroSalary_throws() {
        assertThatThrownBy(() -> AvantagesConventionnelsBeCalculator.compute(
                BigDecimal.ZERO, 220, 24, "CP_200", 2026,
                false, true, true, true, true, 220))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("salaire");
    }

    @Test
    void compute_negativeSalary_throws() {
        assertThatThrownBy(() -> AvantagesConventionnelsBeCalculator.compute(
                new BigDecimal("-100"), 220, 24, "CP_200", 2026,
                false, true, true, true, true, 220))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_negativeAnciennete_throws() {
        assertThatThrownBy(() -> AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, -1, "CP_200", 2026,
                false, true, true, true, true, 220))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anciennette");
    }

    @Test
    void compute_negativeJoursPrestes_throws() {
        assertThatThrownBy(() -> AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, true, true, true, true, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("joursPrestes");
    }

    @Test
    void compute_yearTooEarly_throws() {
        assertThatThrownBy(() -> AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 1970,
                false, true, true, true, true, 220))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("annee");
    }

    @Test
    void compute_nullCommissionParitaire_throws() {
        assertThatThrownBy(() -> AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, null, 2026,
                false, true, true, true, true, 220))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commissionParitaire");
    }

    @Test
    void compute_unknownCommissionParitaire_throws() {
        assertThatThrownBy(() -> AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_999", 2026,
                false, true, true, true, true, 220))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("commissionParitaire");
    }

    @Test
    void compute_totalEqualsSumOf5Lines() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                new BigDecimal("2500.00"), 200, 36, "CP_124", 2026,
                false, true, true, true, true, 210);

        BigDecimal sum = r.peculeVacancesSimpleEur()
                .add(r.doublePeculeVacancesEur())
                .add(r.primeFinAnneeEur())
                .add(r.ecoChequesValeurAnnuelleEur())
                .add(r.chequesRepasValeurAnnuelleEur());
        assertThat(r.totalAvantagesAnnuelsEur()).isEqualByComparingTo(sum);
    }

    @Test
    void compute_messagesContainLegalReferences() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, true, true, true, true, 220);

        assertThat(r.messages()).isNotEmpty();
        assertThat(String.join(" | ", r.messages()))
                .contains("Loi 28/06/1971")
                .contains("CCT 98")
                .contains("CCT 19octies");
    }

    @Test
    void compute_ecoChequesValiditeFalse_messageWarnsUtilisation() {
        AvantagesConventionnelsBeResult r = AvantagesConventionnelsBeCalculator.compute(
                SALAIRE_3000, 220, 24, "CP_200", 2026,
                false, false, true, false, false, 0);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m.toLowerCase()).contains("24 mois"));
    }
}
