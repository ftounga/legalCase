package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.RegimeBeSeparationBiensCalculator.RegimeBeSeparationBiensVerdict;
import fr.ailegalcase.casefile.RegimeBeSeparationBiensCalculator.VarianteRegime;

/**
 * SF-223-06 : tests unitaires du moteur décisionnel BE du régime de séparation
 * de biens — 3 variantes (pure / société d'acquêts / participation aux acquêts),
 * créance de participation, correctif équitable 2018 + gates.
 */
class RegimeBeSeparationBiensCalculatorTest {

    private static RegimeBeSeparationBiensInput input(VarianteRegime variante, Boolean clause) {
        return new RegimeBeSeparationBiensInput(
                variante, true, clause, false, LocalDate.of(2020, 1, 1), null, null);
    }

    @Test
    void separation_pure_qualifiee() {
        RegimeBeSeparationBiensResult r = RegimeBeSeparationBiensCalculator.compute(
                input(VarianteRegime.SEPARATION_PURE, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(RegimeBeSeparationBiensVerdict.SEPARATION_PURE_QUALIFIEE);
        assertThat(r.effetsPatrimoniaux().toLowerCase()).contains("aucune masse commune");
    }

    @Test
    void societe_acquets_qualifiee() {
        RegimeBeSeparationBiensResult r = RegimeBeSeparationBiensCalculator.compute(
                input(VarianteRegime.SEPARATION_AVEC_SOCIETE_ACQUETS, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(RegimeBeSeparationBiensVerdict.SOCIETE_ACQUETS_QUALIFIEE);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("acquêts"));
    }

    @Test
    void participation_acquets_qualifiee_creance_indiquee() {
        RegimeBeSeparationBiensResult r = RegimeBeSeparationBiensCalculator.compute(
                input(VarianteRegime.SEPARATION_AVEC_PARTICIPATION_ACQUETS, true), "BELGIQUE");
        assertThat(r.verdict())
                .isEqualTo(RegimeBeSeparationBiensVerdict.PARTICIPATION_ACQUETS_QUALIFIEE);
        // Créance de participation explicitement indiquée + renvoi liquidation-partage-be.
        assertThat(r.effetsPatrimoniaux().toLowerCase()).contains("créance de participation");
        assertThat(r.messages()).anyMatch(m -> m.contains("liquidation-partage-be"));
    }

    @Test
    void participation_avec_patrimoines_connus_calculable() {
        RegimeBeSeparationBiensInput in = new RegimeBeSeparationBiensInput(
                VarianteRegime.SEPARATION_AVEC_PARTICIPATION_ACQUETS, true, true, false,
                LocalDate.of(2019, 6, 1), 120000d, 80000d);
        RegimeBeSeparationBiensResult r = RegimeBeSeparationBiensCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict())
                .isEqualTo(RegimeBeSeparationBiensVerdict.PARTICIPATION_ACQUETS_QUALIFIEE);
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("calculable"));
    }

    @Test
    void correctif_equitable_2018_mentionne_si_disproportion() {
        RegimeBeSeparationBiensInput in = new RegimeBeSeparationBiensInput(
                VarianteRegime.SEPARATION_PURE, true, null, true, LocalDate.of(2020, 1, 1),
                null, null);
        RegimeBeSeparationBiensResult r = RegimeBeSeparationBiensCalculator.compute(in, "BELGIQUE");
        assertThat(r.motifs()).anyMatch(m -> m.toLowerCase().contains("correction judiciaire en équité"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("22/07/2018"));
    }

    @Test
    void variante_absente_leve_400() {
        RegimeBeSeparationBiensInput in = new RegimeBeSeparationBiensInput(
                null, true, null, false, null, null, null);
        assertThatThrownBy(() -> RegimeBeSeparationBiensCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void clause_participation_absente_pour_variante_participation_leve_400() {
        RegimeBeSeparationBiensInput in = new RegimeBeSeparationBiensInput(
                VarianteRegime.SEPARATION_AVEC_PARTICIPATION_ACQUETS, true, null, false,
                LocalDate.of(2020, 1, 1), null, null);
        assertThatThrownBy(() -> RegimeBeSeparationBiensCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void date_future_leve_400() {
        RegimeBeSeparationBiensInput in = new RegimeBeSeparationBiensInput(
                VarianteRegime.SEPARATION_PURE, true, null, false, LocalDate.now().plusDays(1),
                null, null);
        assertThatThrownBy(() -> RegimeBeSeparationBiensCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void patrimoine_negatif_leve_400() {
        RegimeBeSeparationBiensInput in = new RegimeBeSeparationBiensInput(
                VarianteRegime.SEPARATION_PURE, true, null, false, null, -1d, null);
        assertThatThrownBy(() -> RegimeBeSeparationBiensCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() -> RegimeBeSeparationBiensCalculator.compute(
                input(VarianteRegime.SEPARATION_PURE, null), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        RegimeBeSeparationBiensResult r = RegimeBeSeparationBiensCalculator.compute(
                input(VarianteRegime.SEPARATION_PURE, null), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
        assertThat(r.motifs()).noneMatch(m -> m.toUpperCase().contains("ECLI"));
        // Renvoi explicite vers liquidation-partage-be (F-217) pour le chiffrage.
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("liquidation-partage-be"));
    }
}
