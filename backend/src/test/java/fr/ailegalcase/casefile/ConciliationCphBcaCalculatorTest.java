package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.ConciliationCphBcaCalculator.PalierBca;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-37 — tests unitaires du calculateur de conciliation CPH BCA
 * (F-DT-84-conciliation-cph-bca, FRANCE — R. 1454-7 à R. 1454-12 CT ;
 * L. 1235-1 al. 3 CT — barème transactions BCA).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : palier BCA selon
 * ancienneté, montant minimum, comparaison BCA vs Macron, checklist BCO,
 * gate country FRANCE, opportunité.</p>
 */
class ConciliationCphBcaCalculatorTest {

    /** Baseline : 36 mois (3 ans), 2 500 €/mois, demandes 30 000 €, opportunité FAVORABLE. */
    private static ConciliationCphBcaInput baseline() {
        return new ConciliationCphBcaInput(
                36,
                2500.0,
                30000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.FAVORABLE
        );
    }

    // ── AC1 — Ancienneté 10 mois → palier < 2 ans, 2 mois × salaire ──────────

    @Test
    void anciennete10Mois_palier2Mois_montant2Mois() {
        var input = new ConciliationCphBcaInput(
                10, 2000.0, 20000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.palierBcaApplicable()).contains("< 2 ans").contains("2 mois");
        assertThat(r.montantMinimumBcaEuros()).isEqualTo(4000.0);
    }

    // ── AC2 — Ancienneté 12 ans → palier "8 à 15 ans : 4 mois" ───────────────

    @Test
    void anciennete12Ans_palier4Mois() {
        var input = new ConciliationCphBcaInput(
                12 * 12, 3000.0, 50000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.FAVORABLE);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.palierBcaApplicable()).isEqualTo("8 à 15 ans : 4 mois");
        assertThat(r.montantMinimumBcaEuros()).isEqualTo(12000.0);
    }

    // ── AC3 — Tous les paliers ───────────────────────────────────────────────

    @Test
    void anciennete0Mois_palierMoinsDe2Ans() {
        assertThat(ConciliationCphBcaCalculator.palierBcaApplicable(0))
                .isEqualTo(PalierBca.MOINS_DE_2_ANS);
    }

    @Test
    void anciennete23Mois_palierMoinsDe2Ans() {
        assertThat(ConciliationCphBcaCalculator.palierBcaApplicable(23))
                .isEqualTo(PalierBca.MOINS_DE_2_ANS);
    }

    @Test
    void anciennete24Mois_palier2A8Ans() {
        assertThat(ConciliationCphBcaCalculator.palierBcaApplicable(24))
                .isEqualTo(PalierBca.DE_2_A_8_ANS);
    }

    @Test
    void anciennete7Ans11Mois_palier2A8Ans() {
        assertThat(ConciliationCphBcaCalculator.palierBcaApplicable(7 * 12 + 11))
                .isEqualTo(PalierBca.DE_2_A_8_ANS);
    }

    @Test
    void anciennete8Ans_palier8A15Ans() {
        assertThat(ConciliationCphBcaCalculator.palierBcaApplicable(8 * 12))
                .isEqualTo(PalierBca.DE_8_A_15_ANS);
    }

    @Test
    void anciennete15Ans_palier15A25Ans() {
        assertThat(ConciliationCphBcaCalculator.palierBcaApplicable(15 * 12))
                .isEqualTo(PalierBca.DE_15_A_25_ANS);
    }

    @Test
    void anciennete25Ans_palierAuMoins25Ans() {
        assertThat(ConciliationCphBcaCalculator.palierBcaApplicable(25 * 12))
                .isEqualTo(PalierBca.AU_MOINS_25_ANS);
    }

    @Test
    void anciennete40Ans_palierAuMoins25Ans_montant14Mois() {
        var input = new ConciliationCphBcaInput(
                40 * 12, 4000.0, 100000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.palierBcaApplicable()).contains("≥ 25 ans").contains("14 mois");
        assertThat(r.montantMinimumBcaEuros()).isEqualTo(56000.0);
    }

    // ── AC4 — Comparaison BCA vs Macron ──────────────────────────────────────

    @Test
    void demandesNulles_comparaisonMessageDefaut() {
        var input = new ConciliationCphBcaInput(
                36, 2500.0, 0.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.comparaisonBcaVsMacron()).contains("non renseigné");
    }

    @Test
    void bcaSuperieurAuxDemandes_comparaisonSecuritePV() {
        // 10 ans → 4 mois × 5 000 = 20 000 €. Demandes = 15 000 € → BCA ≥ demandes.
        var input = new ConciliationCphBcaInput(
                10 * 12, 5000.0, 15000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.FAVORABLE);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.comparaisonBcaVsMacron()).contains("supérieur ou égal").contains("sécurise");
    }

    @Test
    void bcaInferieurAuxDemandes_comparaisonArbitrage() {
        // 10 mois → 2 mois × 2 000 = 4 000 €. Demandes = 30 000 € → BCA << demandes.
        var input = new ConciliationCphBcaInput(
                10, 2000.0, 30000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.comparaisonBcaVsMacron()).contains("inférieur").contains("écart");
    }

    // ── AC5 — Checklist BCO non vide ─────────────────────────────────────────

    @Test
    void checklistBcoNonVide() {
        var r = ConciliationCphBcaCalculator.compute(baseline(), "FRANCE");
        assertThat(r.checklistBco()).isNotEmpty();
        assertThat(r.checklistBco()).anyMatch(c -> c.contains("Requête introductive"));
        assertThat(r.checklistBco()).anyMatch(c -> c.contains("Bordereau de pièces"));
        assertThat(r.checklistBco()).anyMatch(c -> c.contains("Contrat de travail"));
    }

    // ── AC6 — Bases juridiques pertinentes ───────────────────────────────────

    @Test
    void basesJuridiques_contiennentFondementsBcaEtBco() {
        var r = ConciliationCphBcaCalculator.compute(baseline(), "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1235-1"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("R. 1454"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1411-1"));
    }

    // ── AC7 — Opportunité — 3 niveaux ────────────────────────────────────────

    @Test
    void opportuniteFavorable_messageOrientationConciliation() {
        var input = new ConciliationCphBcaInput(
                36, 2500.0, 30000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.FAVORABLE);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("FAVORABLE"));
    }

    @Test
    void opportuniteDefavorable_messageBureauJugement() {
        var input = new ConciliationCphBcaInput(
                36, 2500.0, 30000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.DEFAVORABLE);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("DÉFAVORABLE"));
    }

    @Test
    void opportuniteIncertaine_messageMiseEnEtat() {
        var input = new ConciliationCphBcaInput(
                36, 2500.0, 30000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("INCERTAINE"));
    }

    @Test
    void opportuniteNull_defautIncertaine() {
        var input = new ConciliationCphBcaInput(
                36, 2500.0, 30000.0, null);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("INCERTAINE"));
    }

    // ── AC8 — Gate country FRANCE ────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ConciliationCphBcaCalculator.compute(baseline(), "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void countryNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ConciliationCphBcaCalculator.compute(baseline(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                ConciliationCphBcaCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void countryFranceMinuscule_accepteCaseInsensitive() {
        var r = ConciliationCphBcaCalculator.compute(baseline(), "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    // ── AC9 — Champs null tolérés (0 par défaut) ────────────────────────────

    @Test
    void salaireNull_montantMinimumZero() {
        var input = new ConciliationCphBcaInput(
                36, null, 30000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.montantMinimumBcaEuros()).isEqualTo(0.0);
    }

    @Test
    void ancienneteNull_palierMoinsDe2Ans() {
        var input = new ConciliationCphBcaInput(
                null, 2500.0, 30000.0,
                ConciliationCphBcaInput.ConciliationCphBcaOpportunite.INCERTAIN);
        var r = ConciliationCphBcaCalculator.compute(input, "FRANCE");
        assertThat(r.palierBcaApplicable()).contains("< 2 ans");
    }

    // ── AC10 — Country dans le résultat ──────────────────────────────────────

    @Test
    void country_estFranceDansResultat() {
        var r = ConciliationCphBcaCalculator.compute(baseline(), "FRANCE");
        assertThat(r.country()).isEqualTo("FRANCE");
    }
}
