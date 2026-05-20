package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-216-01 : tests unitaires du calculateur prestation compensatoire.
 * Couvre les règles art. 270-281 Cciv + Cass. 1ère civ. 8/10/2014 n°13-21.804.
 */
class PrestationCompensatoireCalculatorSF216Test {

    // ── Cas nominal — forme CAPITAL (demandeur < 60 ans, durée < 20 ans) ────────

    @Test
    void capital_jeune_courte_duree() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                10, 60_000, 20_000, null, null, 45, 42, null, false, "FRANCE");
        assertThat(r.formeRecommandee()).isEqualTo(FormePrestationEnum.CAPITAL);
        assertThat(r.montantCapitalIndicatifEur()).isGreaterThan(0);
        assertThat(r.renteIndicativeMensuelleEur()).isEqualTo(0);
        assertThat(r.alertes()).isEmpty();
    }

    // ── Cas nominal — forme RENTE (durée ≥ 20 ans) ───────────────────────────

    @Test
    void rente_longue_duree() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                25, 50_000, 20_000, null, null, 50, 48, null, false, "FRANCE");
        assertThat(r.formeRecommandee()).isEqualTo(FormePrestationEnum.RENTE);
        assertThat(r.renteIndicativeMensuelleEur()).isGreaterThan(0);
        assertThat(r.montantCapitalIndicatifEur()).isEqualTo(0);
    }

    // ── Cas nominal — forme RENTE (demandeur ≥ 60 ans, durée < 20 ans) ─────────

    @Test
    void rente_demandeur_senior() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                15, 40_000, 20_000, null, null, 62, 58, null, false, "FRANCE");
        assertThat(r.formeRecommandee()).isEqualTo(FormePrestationEnum.RENTE);
        assertThat(r.renteIndicativeMensuelleEur()).isGreaterThan(0);
    }

    // ── Avantage matrimonial — réduction 20 % ────────────────────────────────

    @Test
    void avantageMatrimonial_reduit_montant() {
        PrestationCompensatoireResult sans = PrestationCompensatoireCalculatorSF216.compute(
                10, 60_000, 20_000, null, null, 45, 42, null, false, "FRANCE");
        PrestationCompensatoireResult avec = PrestationCompensatoireCalculatorSF216.compute(
                10, 60_000, 20_000, null, null, 45, 42, null, true, "FRANCE");
        assertThat(avec.montantCapitalIndicatifEur())
                .isLessThan(sans.montantCapitalIndicatifEur());
    }

    // ── Disparité de niveaux de vie ───────────────────────────────────────────

    @Test
    void disparite_elevee_message_present() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                15, 100_000, 15_000, null, null, 50, 47, null, false, "FRANCE");
        assertThat(r.dispAriteNiveauxVie()).isNotBlank();
        assertThat(r.messages()).isNotEmpty();
    }

    // ── Revenus égaux — capital nul ───────────────────────────────────────────

    @Test
    void revenus_egaux_capital_zero() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                10, 40_000, 40_000, null, null, 45, 43, null, false, "FRANCE");
        assertThat(r.montantCapitalIndicatifEur()).isEqualTo(0);
        assertThat(r.renteIndicativeMensuelleEur()).isEqualTo(0);
    }

    // ── Base juridique ────────────────────────────────────────────────────────

    @Test
    void baseJuridique_contient_art270() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                10, 60_000, 20_000, null, null, 45, 42, null, false, "FRANCE");
        assertThat(r.baseJuridique()).contains("270");
    }

    // ── Gate pays — hors FRANCE lève IllegalArgumentException ────────────────

    @Test
    void pays_horsfrfance_leve_exception() {
        assertThatThrownBy(() -> PrestationCompensatoireCalculatorSF216.compute(
                10, 60_000, 20_000, null, null, 45, 42, null, false, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── Mariage court — alerte présente ──────────────────────────────────────

    @Test
    void mariage_court_alerte_presente() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                1, 60_000, 20_000, null, null, 35, 33, null, false, "FRANCE");
        assertThat(r.alertes()).anyMatch(a -> a.contains("courte durée"));
    }

    // ── Revenus inférieurs époux 1 — forme INCERTAIN ──────────────────────────

    @Test
    void revenus1_superieurs_incertain() {
        PrestationCompensatoireResult r = PrestationCompensatoireCalculatorSF216.compute(
                10, 80_000, 20_000, null, null, 45, 42, null, false, "FRANCE");
        // revenus2 < revenus1 → pas de disparité en faveur du demandeur
        assertThat(r.formeRecommandee()).isEqualTo(FormePrestationEnum.INCERTAIN);
        assertThat(r.montantCapitalIndicatifEur()).isEqualTo(0);
    }
}
