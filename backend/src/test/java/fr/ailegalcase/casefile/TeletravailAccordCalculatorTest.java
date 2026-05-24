package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static fr.ailegalcase.casefile.TeletravailAccordCalculator.AnalyseTeletravail;
import static fr.ailegalcase.casefile.TeletravailAccordInput.CadreTeletravail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-15 — tests unitaires du calculateur d'analyse de la conformité du
 * dispositif de télétravail et des litiges courants
 * (F-DT-82-teletravail-accord, FRANCE — L. 1222-9 à L. 1222-11 CT ; ANI
 * télétravail du 26/11/2020).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : verdict
 * CONFORME / NON_CONFORME / LITIGE_IDENTIFIE ; alertes accident à domicile
 * et refus-licenciement ; estimation indemnité due ; gate country FRANCE.</p>
 */
class TeletravailAccordCalculatorTest {

    // ── AC1 : cadre conforme + double volontariat + indemnité → CONFORME ─────

    @Test
    void accordCollectif_doubleVolontariat_indemniteVersee_returnsConforme() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.CONFORME);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.scoreTeletravail()).isGreaterThanOrEqualTo(80);
        assertThat(r.alerteAccidentTravailDomicile()).isFalse();
        assertThat(r.alerteRefusTeletravailLicenciement()).isFalse();
    }

    @Test
    void charteUnilaterale_doubleVolontariat_indemniteVersee_returnsConforme() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.CHARTE_UNILATERALE, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.CONFORME);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1222-9 al. 2"));
    }

    @Test
    void accordIndividuel_doubleVolontariat_indemniteVersee_returnsConforme() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_INDIVIDUEL, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.CONFORME);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1222-9 al. 3"));
    }

    // ── AC2 : aucun cadre juridique → NON_CONFORME ──────────────────────────

    @Test
    void aucunCadre_returnsNonConforme() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.AUCUN, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.NON_CONFORME);
        assertThat(r.scoreTeletravail()).isLessThanOrEqualTo(20);
        assertThat(r.messages()).anyMatch(m -> m.contains("L. 1222-9"));
    }

    // ── AC3 : double volontariat non respecté → NON_CONFORME ────────────────

    @Test
    void cadreOk_volontariatViole_sansLitige_returnsNonConforme() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, false, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.NON_CONFORME);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1222-11"));
    }

    // ── AC4 : indemnité non versée → NON_CONFORME + estimation indemnité due ─

    @Test
    void cadreOk_indemniteNonVersee_returnsNonConforme_avecIndemniteDue() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, false, null, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.NON_CONFORME);
        assertThat(r.indemniteDueEstimeeEuros()).isNotNull();
        assertThat(r.indemniteDueEstimeeEuros()).isGreaterThan(0.0);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("URSSAF"));
    }

    // ── AC5 : accident à domicile → LITIGE_IDENTIFIE + alerte AT ─────────────

    @Test
    void cadreOk_accidentDomicile_returnsLitigeIdentifie_avecAlerteAt() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, true, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.LITIGE_IDENTIFIE);
        assertThat(r.alerteAccidentTravailDomicile()).isTrue();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1222-9 al. 4"));
        assertThat(r.messages()).anyMatch(m -> m.contains("présomption"));
    }

    // ── AC6 : refus invoqué comme cause → LITIGE_IDENTIFIE + alerte refus ────

    @Test
    void cadreOk_refusInvoque_returnsLitigeIdentifie_avecAlerteRefus() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, false, true);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.LITIGE_IDENTIFIE);
        assertThat(r.alerteRefusTeletravailLicenciement()).isTrue();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1222-9 al. 6"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("interdit"));
    }

    // ── Retour bureau imposé → LITIGE_IDENTIFIE ──────────────────────────────

    @Test
    void cadreOk_retourBureauImpose_returnsLitigeIdentifie() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, true, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.LITIGE_IDENTIFIE);
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1222-10"));
    }

    // ── Alertes distinctes ──────────────────────────────────────────────────

    @Test
    void alerteAccident_etRefus_sontDistinctes() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, true, false, true);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.alerteAccidentTravailDomicile()).isTrue();
        assertThat(r.alerteRefusTeletravailLicenciement()).isTrue();
    }

    // ── Pas de litige sans condition ─────────────────────────────────────────

    @Test
    void sansLitige_alertesFalse() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.alerteAccidentTravailDomicile()).isFalse();
        assertThat(r.alerteRefusTeletravailLicenciement()).isFalse();
    }

    // ── Erreurs / pays ───────────────────────────────────────────────────────

    @Test
    void countryBelgique_throwsIllegalArgument() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, false, false);
        assertThatThrownBy(() ->
                TeletravailAccordCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throwsIllegalArgument() {
        assertThatThrownBy(() ->
                TeletravailAccordCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("manquantes");
    }

    @Test
    void cadreNull_throwsIllegalArgument() {
        var input = new TeletravailAccordInput(
                null, true, true, 2.60, false, false, false);
        assertThatThrownBy(() ->
                TeletravailAccordCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cadre");
    }

    @Test
    void montantNegatif_throwsIllegalArgument() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, -1.0, false, false, false);
        assertThatThrownBy(() ->
                TeletravailAccordCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("négatif");
    }

    // ── Score borné + bases juridiques de référence ──────────────────────────

    @Test
    void score_borneEntreZeroEtCent() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.scoreTeletravail()).isBetween(0, 100);
    }

    @Test
    void basesJuridiques_contiennentReferencesAttendues() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1222-9"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("ANI"));
    }

    // ── Points d'analyse F-IA-03 ─────────────────────────────────────────────

    @Test
    void pointsAnalyse_couvrentLesQuatreCriteres() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, false, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.pointsTeletravail())
                .extracting(p -> p.code().name())
                .containsExactlyInAnyOrder(
                        "DT82_CADRE_JURIDIQUE",
                        "DT82_DOUBLE_VOLONTARIAT",
                        "DT82_INDEMNITE_OCCUPATION",
                        "DT82_ACCIDENT_DOMICILE");
    }

    // ── Indemnité due non versée + litige : indemnité estimée non null ───────

    @Test
    void indemniteNonVersee_avecLitige_indemniteEstimeeReportee() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, false, null, true, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        // litige caractérisé (accident) → LITIGE_IDENTIFIE
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.LITIGE_IDENTIFIE);
        // indemnité due reportée même si litige (cumul)
        assertThat(r.indemniteDueEstimeeEuros()).isNotNull();
    }

    @Test
    void indemniteVersee_avecLitige_indemniteEstimeeNull() {
        var input = new TeletravailAccordInput(
                CadreTeletravail.ACCORD_COLLECTIF, true, true, 2.60, true, false, false);
        var r = TeletravailAccordCalculator.compute(input, "FRANCE");
        assertThat(r.analyseTeletravail()).isEqualTo(AnalyseTeletravail.LITIGE_IDENTIFIE);
        assertThat(r.indemniteDueEstimeeEuros()).isNull();
    }
}
