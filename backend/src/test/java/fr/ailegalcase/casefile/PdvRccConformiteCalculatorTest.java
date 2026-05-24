package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.PdvRccConformiteCalculator.AnalysePdvRcc;
import static fr.ailegalcase.casefile.PdvRccConformiteInput.TypeDispositif;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-35 — tests unitaires du calculateur d'analyse de la conformité d'un
 * PDV / RCC (F-DT-46-pdv-rcc-conformite, FRANCE).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * CONFORME / PARTIELLEMENT_CONFORME / NON_CONFORME ; conditions RCC
 * (accord majoritaire + validation DREETS) ; indemnités plancher ;
 * adhésion volontaire ; droit ARE confirmé ; gate country FRANCE.</p>
 */
class PdvRccConformiteCalculatorTest {

    // ── AC1 : RCC complète et conforme → CONFORME + droit ARE ───────────────

    @Test
    void rccComplete_returnsConforme_avecDroitAre() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                true,   // accord majoritaire
                true,   // validation DREETS obtenue
                true,   // délai 15 j respecté
                true,   // indemnités ≥ légales
                true,   // adhésion volontaire
                true    // information complète
        );
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.CONFORME);
        assertThat(r.droitAreConfirme()).isTrue();
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.scoreConformite()).isEqualTo(100);
        assertThat(r.pointsIrregularite()).isEmpty();
    }

    @Test
    void rccConforme_messageMentionneDroitAre() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC, true, true, true, true, true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.messages())
                .anyMatch(m -> m.toUpperCase().contains("ARE"));
    }

    // ── AC2 : accord non majoritaire → NON_CONFORME ─────────────────────────

    @Test
    void rccAccordNonMajoritaire_returnsNonConforme() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                false,  // accord NON majoritaire
                true, true, true, true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.NON_CONFORME);
        assertThat(r.droitAreConfirme()).isFalse();
        assertThat(r.pointsIrregularite())
                .anyMatch(p -> p.code().name().equals("DT46_ACCORD_MAJORITAIRE"));
    }

    // ── AC3 : validation DREETS manquante → NON_CONFORME ────────────────────

    @Test
    void rccValidationDreetsManquante_returnsNonConforme() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                true,
                false, // validation DREETS NON obtenue
                null,
                true, true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.NON_CONFORME);
        assertThat(r.droitAreConfirme()).isFalse();
        assertThat(r.pointsIrregularite())
                .anyMatch(p -> p.code().name().equals("DT46_VALIDATION_DREETS"));
    }

    // ── AC4 : délai DREETS > 15 j → point d'irrégularité mais reste conforme

    @Test
    void rccDelaiDREETSDepasse_returnsPartiellementConforme() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                true,
                true,
                false, // délai 15 j NON respecté
                true, true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.PARTIELLEMENT_CONFORME);
        // RCC partiellement conforme — droit ARE non confirmé (réservé CONFORME).
        assertThat(r.droitAreConfirme()).isFalse();
        assertThat(r.pointsIrregularite())
                .anyMatch(p -> p.code().name().equals("DT46_VALIDATION_DREETS"));
    }

    // ── AC5 : indemnités < plancher légal → NON_CONFORME ────────────────────

    @Test
    void indemnitesInferieuresAuxLegales_returnsNonConforme() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                true, true, true,
                false, // indemnités < légales
                true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.NON_CONFORME);
        assertThat(r.pointsIrregularite())
                .anyMatch(p -> p.code().name().equals("DT46_INDEMNITEES"));
    }

    // ── AC6 : adhésion non volontaire → NON_CONFORME ────────────────────────

    @Test
    void adhesionNonVolontaire_returnsNonConforme() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                true, true, true, true,
                false, // adhésion NON volontaire
                true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.NON_CONFORME);
        assertThat(r.pointsIrregularite())
                .anyMatch(p -> p.code().name().equals("DT46_ADHESION_VOLONTAIRE"));
    }

    // ── AC7 : information incomplète seule → PARTIELLEMENT_CONFORME ─────────

    @Test
    void informationIncomplete_returnsPartiellementConforme() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                true, true, true, true, true,
                false // information incomplète
        );
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.PARTIELLEMENT_CONFORME);
        assertThat(r.droitAreConfirme()).isFalse();
    }

    // ── AC8 : PDV (hors RCC) conforme → CONFORME mais PAS de droit ARE ──────

    @Test
    void pdvConforme_returnsConforme_maisSansDroitAreSpecifique() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.PDV,
                false, // accord majoritaire non requis pour PDV
                false, // pas de validation DREETS pour PDV unilatéral
                null,
                true, true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.CONFORME);
        assertThat(r.droitAreConfirme())
                .as("PDV hors RCC : droit ARE non confirmé par cet outil")
                .isFalse();
    }

    @Test
    void pdvIndemnitesInsuffisantes_returnsNonConforme() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.PDV,
                false, false, null,
                false, // indemnités < légales
                true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.analysePdvRcc()).isEqualTo(AnalysePdvRcc.NON_CONFORME);
    }

    // ── AC9 : gate country ──────────────────────────────────────────────────

    @Test
    void belgique_throwsIllegalArgument() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC, true, true, true, true, true, true);
        assertThatThrownBy(() -> PdvRccConformiteCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throws() {
        assertThatThrownBy(() -> PdvRccConformiteCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void typeDispositifNull_throws() {
        var input = new PdvRccConformiteInput(
                null, true, true, true, true, true, true);
        assertThatThrownBy(() -> PdvRccConformiteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositif");
    }

    // ── AC10 : facteurs F-IA-03 — 4 codes critères possibles ────────────────

    @Test
    void cumulIrregularites_quatreCodes_critereTous_presents() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC,
                false, // ACCORD
                false, // DREETS
                null,
                false, // INDEMNITES
                false, // ADHESION
                false);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.pointsIrregularite())
                .extracting(f -> f.code().name())
                .contains(
                        "DT46_ACCORD_MAJORITAIRE",
                        "DT46_VALIDATION_DREETS",
                        "DT46_INDEMNITEES",
                        "DT46_ADHESION_VOLONTAIRE");
    }

    // ── AC11 : bases juridiques contiennent les fondements clés ─────────────

    @Test
    void basesJuridiques_contiennentReferencesClef() {
        var input = new PdvRccConformiteInput(
                TypeDispositif.RCC, true, true, true, true, true, true);
        var r = PdvRccConformiteCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1237-19"));
    }
}
