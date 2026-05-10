package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-210-03 : tests unitaires de
 * {@link AcceptationRenonciationSuccessionCalculator}.
 */
class AcceptationRenonciationSuccessionCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 5, 1);

    @Test
    void premier_rang_actif_positif_recommande_pure_simple() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG",
                250000, 50000,
                false, false, false, "INCERTAIN", TODAY);
        assertThat(r.optionsOuvertes()).hasSize(3);
        assertThat(r.optionRecommandee())
                .isEqualTo(AcceptationRenonciationSuccessionCalculator.OPTION_PURE_SIMPLE);
        assertThat(r.delaiTotalJours()).isEqualTo(120);
        assertThat(r.delaiRestantJours()).isEqualTo(120 - 30);
    }

    @Test
    void second_rang_a_180_jours() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "SECOND_RANG",
                100000, 0,
                false, false, false, "INCERTAIN", TODAY);
        assertThat(r.delaiTotalJours()).isEqualTo(180);
    }

    @Test
    void delai_depasse_renvoie_negatif_et_message() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2025, 1, 1), "PREMIER_RANG",
                100000, 50000,
                false, false, false, "INCERTAIN", TODAY);
        assertThat(r.delaiRestantJours()).isLessThan(0);
        assertThat(r.messages()).anyMatch(m -> m.contains("dépass"));
    }

    @Test
    void actes_equivalents_ferment_la_renonciation() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG",
                100000, 30000,
                true, false, false, "INCERTAIN", TODAY);
        assertThat(r.optionsOuvertes())
                .containsExactly(AcceptationRenonciationSuccessionCalculator.OPTION_PURE_SIMPLE);
        assertThat(r.optionRecommandee())
                .isEqualTo(AcceptationRenonciationSuccessionCalculator.OPTION_PURE_SIMPLE);
        assertThat(r.formule()).contains("783");
    }

    @Test
    void passif_superieur_actif_recommande_renonciation() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG",
                10000, 80000,
                false, false, false, "INCERTAIN", TODAY);
        assertThat(r.optionRecommandee())
                .isEqualTo(AcceptationRenonciationSuccessionCalculator.OPTION_RENONCIATION);
    }

    @Test
    void dettes_incertaines_recommande_concurrence_actif() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG",
                100000, 20000,
                false, false, true, "INCERTAIN", TODAY);
        assertThat(r.optionRecommandee())
                .isEqualTo(AcceptationRenonciationSuccessionCalculator.OPTION_CONCURRENCE_ACTIF);
    }

    @Test
    void actif_passif_proches_recommande_concurrence_actif() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG",
                100000, 70000,
                false, false, false, "INCERTAIN", TODAY);
        assertThat(r.optionRecommandee())
                .isEqualTo(AcceptationRenonciationSuccessionCalculator.OPTION_CONCURRENCE_ACTIF);
    }

    @Test
    void intention_divergente_genere_message() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG",
                10000, 80000,
                false, false, false,
                AcceptationRenonciationSuccessionCalculator.OPTION_PURE_SIMPLE,
                TODAY);
        assertThat(r.optionRecommandee())
                .isEqualTo(AcceptationRenonciationSuccessionCalculator.OPTION_RENONCIATION);
        assertThat(r.messages()).anyMatch(m -> m.contains("diverge"));
    }

    @Test
    void date_ouverture_null_throws() {
        assertThatThrownBy(() -> AcceptationRenonciationSuccessionCalculator.compute(
                null, "PREMIER_RANG", 0, 0, false, false, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void date_future_throws() {
        assertThatThrownBy(() -> AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2027, 1, 1), "PREMIER_RANG", 0, 0, false, false, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actif_negatif_throws() {
        assertThatThrownBy(() -> AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG", -10, 0, false, false, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void base_juridique_cite_articles_clefs() {
        AcceptationRenonciationSuccessionResult r = AcceptationRenonciationSuccessionCalculator.compute(
                LocalDate.of(2026, 4, 1), "PREMIER_RANG",
                100000, 0, false, false, false, "INCERTAIN", TODAY);
        assertThat(r.baseJuridique()).contains("768");
        assertThat(r.baseJuridique()).contains("771");
    }
}
