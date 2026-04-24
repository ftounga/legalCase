package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.HarcelementNulliteCalculator.MotifNullite;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HarcelementNulliteCalculatorTest {

    @Test
    void compute_nominalFR_harcelementMoral_returns6moisSalaire() {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("3000.00"), MotifNullite.HARCELEMENT_MORAL, "FRANCE");
        assertThat(r.indemniteMinimumNullite()).isEqualByComparingTo("18000.00");
        assertThat(r.salaireMensuelReference()).isEqualByComparingTo("3000.00");
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.motifNullite()).isEqualTo(MotifNullite.HARCELEMENT_MORAL);
        assertThat(r.baseJuridique()).contains("L.1235-3-1").contains("L.1152-3");
        assertThat(r.messages().get(0)).contains("Plancher légal 6 mois");
        assertThat(r.messages().get(1)).contains("Cumul");
    }

    @Test
    void compute_nominalBE_harcelementMoral_returns6moisSalaire() {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("3000.00"), MotifNullite.HARCELEMENT_MORAL_BE, "BELGIQUE");
        assertThat(r.indemniteMinimumNullite()).isEqualByComparingTo("18000.00");
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.baseJuridique()).contains("Loi 4 août 1996").contains("32tredecies");
    }

    @Test
    void compute_nominalBE_discrimination_returnsLoi2007AsBase() {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("2500.00"), MotifNullite.DISCRIMINATION_BE, "BELGIQUE");
        assertThat(r.indemniteMinimumNullite()).isEqualByComparingTo("15000.00");
        assertThat(r.baseJuridique()).contains("Loi 10 mai 2007").contains("anti-discrimination");
    }

    @Test
    void compute_motifFR_onBELGIQUE_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                new BigDecimal("3000"), MotifNullite.HARCELEMENT_MORAL, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void compute_motifBE_onFRANCE_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                new BigDecimal("3000"), MotifNullite.HARCELEMENT_MORAL_BE, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @ParameterizedTest
    @EnumSource(value = MotifNullite.class, names = {
            "HARCELEMENT_MORAL", "HARCELEMENT_SEXUEL", "DISCRIMINATION", "GROSSESSE",
            "SALARIE_PROTEGE", "LIBERTE_FONDAMENTALE", "ACTION_JUSTICE", "ALERTE_ETHIQUE"
    })
    void compute_everyFRMotif_returnsValidResultWithL1235Base(MotifNullite motif) {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("2000.00"), motif, "FRANCE");
        assertThat(r.indemniteMinimumNullite()).isEqualByComparingTo("12000.00");
        assertThat(r.baseJuridique()).contains("L.1235-3-1");
        assertThat(r.motifNullite()).isEqualTo(motif);
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    @ParameterizedTest
    @EnumSource(value = MotifNullite.class, names = {
            "HARCELEMENT_MORAL_BE", "HARCELEMENT_SEXUEL_BE",
            "VIOLENCE_AU_TRAVAIL_BE", "DISCRIMINATION_BE"
    })
    void compute_everyBEMotif_returnsValidResult(MotifNullite motif) {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("2000.00"), motif, "BELGIQUE");
        assertThat(r.indemniteMinimumNullite()).isEqualByComparingTo("12000.00");
        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.motifNullite()).isEqualTo(motif);
    }

    @Test
    void compute_salarieProtege_includesNonCumulLicenciementMessage() {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("3000"), MotifNullite.SALARIE_PROTEGE, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("salarié") && m.contains("protégé"));
    }

    @Test
    void compute_negativeSalaire_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                new BigDecimal("-1"), MotifNullite.HARCELEMENT_MORAL, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("strictement positif");
    }

    @Test
    void compute_zeroSalaire_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                BigDecimal.ZERO, MotifNullite.HARCELEMENT_MORAL, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullSalaire_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                null, MotifNullite.HARCELEMENT_MORAL, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_nullMotif_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                new BigDecimal("3000"), null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motif");
    }

    @Test
    void compute_nullCountry_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                new BigDecimal("3000"), MotifNullite.HARCELEMENT_MORAL, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void compute_unsupportedCountry_throwsIllegalArgument() {
        assertThatThrownBy(() -> HarcelementNulliteCalculator.compute(
                new BigDecimal("3000"), MotifNullite.HARCELEMENT_MORAL, "LUXEMBOURG"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non supporté");
    }

    @Test
    void compute_formula_properlyFormatted() {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("3000.00"), MotifNullite.HARCELEMENT_MORAL, "FRANCE");
        assertThat(r.formule())
                .contains("6 mois")
                .contains("3000,00")
                .contains("18000,00");
    }

    @Test
    void compute_arbitraryAmount_multipliesBy6() {
        HarcelementNulliteResult r = HarcelementNulliteCalculator.compute(
                new BigDecimal("3450.75"), MotifNullite.HARCELEMENT_MORAL, "FRANCE");
        assertThat(r.indemniteMinimumNullite()).isEqualByComparingTo("20704.50");
    }
}
