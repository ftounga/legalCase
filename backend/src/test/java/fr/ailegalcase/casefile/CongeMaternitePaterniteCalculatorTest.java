package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static fr.ailegalcase.casefile.CongeMaternitePaterniteInput.TypeConge;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-212-29 — tests unitaires du calculateur d'analyse du congé maternité /
 * paternité (F-DT-77-conge-paternite-maternite, FRANCE).
 *
 * <p>Couvre les critères d'acceptation de la mini-spec : durée maternité
 * (1er/2e/3e enfant, jumeaux, triplés) ; durée paternité (simple,
 * multiple) ; protection licenciement + 10 sem post-retour ; alertes
 * licenciement et retour poste ; gate country FRANCE.</p>
 */
class CongeMaternitePaterniteCalculatorTest {

    private static final LocalDate DATE_DEBUT = LocalDate.of(2026, 6, 1);

    // ── AC1 : maternité 1er enfant → 112 jours (16 sem) ────────────────────

    @Test
    void maternitePremierEnfant_returns112Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.dureeCongeJours()).isEqualTo(112);
        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.dateFinCongeTheorique()).isEqualTo(DATE_DEBUT.plusDays(112));
    }

    @Test
    void materniteDeuxiemeEnfant_returns112Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 2, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.dureeCongeJours()).isEqualTo(112);
    }

    // ── AC2 : maternité 3e enfant → 182 jours (26 sem) ─────────────────────

    @Test
    void materniteTroisiemeEnfant_returns182Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 3, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.dureeCongeJours()).isEqualTo(182);
    }

    // ── AC3 : jumeaux → 238 jours (34 sem) ─────────────────────────────────

    @Test
    void materniteJumeaux_returns238Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, true, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.dureeCongeJours()).isEqualTo(238); // 34 sem
    }

    @Test
    void materniteTripletsEtPlus_returns322Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 3, true, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.dureeCongeJours()).isEqualTo(322); // 46 sem
    }

    // ── AC4 : paternité simple → 25 jours ──────────────────────────────────

    @Test
    void paterniteSimple_returns25Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.PATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.dureeCongeJours()).isEqualTo(25);
    }

    // ── AC5 : paternité naissance multiple → 32 jours ──────────────────────

    @Test
    void paterniteNaissanceMultiple_returns32Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.PATERNITE, 1, true, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.dureeCongeJours()).isEqualTo(32);
    }

    // ── AC6 : protection licenciement = date fin + 70 jours (10 sem) ───────

    @Test
    void protectionLicenciement_retourPlus70Jours() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.protectionLicenciementJusqua())
                .isEqualTo(r.dateFinCongeTheorique().plusDays(70));
    }

    // ── AC7 : alerteLicenciementIllegal = true si licenciementPendantConge

    @Test
    void licenciementPendantConge_alerte() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                DATE_DEBUT, true, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.alerteLicenciementIllegal()).isTrue();
        assertThat(r.messages()).anyMatch(m -> m.contains("nul de plein droit"));
    }

    @Test
    void retourPosteDifferent_alerte() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, true);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.alerteRetourPosteIllegal()).isTrue();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("L. 1225-25"));
    }

    // ── AC8 : IJ calculée et plafonnée PMSS ────────────────────────────────

    @Test
    void ijJournaliere_calculeeSurSalaireBrut() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.ijJournaliereEstimeeEuros()).isGreaterThan(50.0);
        assertThat(r.ijJournaliereEstimeeEuros()).isLessThan(110.0);
        assertThat(r.ijTotaleEstimeeEuros())
                .isEqualTo(Math.round(r.ijJournaliereEstimeeEuros() * 112 * 100.0) / 100.0);
    }

    @Test
    void ijJournaliere_plafonneeAuPmss() {
        // Salaire très élevé : doit être plafonné au PMSS journalier.
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 50000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        // PMSS 2026 ≈ 3925 / 30.4375 × 0.80 ≈ 103.16 €/j.
        assertThat(r.ijJournaliereEstimeeEuros()).isLessThan(110.0);
    }

    // ── AC9 : gate country ─────────────────────────────────────────────────

    @Test
    void belgique_throwsIllegalArgument() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        assertThatThrownBy(() -> CongeMaternitePaterniteCalculator.compute(input, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE");
    }

    @Test
    void inputNull_throws() {
        assertThatThrownBy(() -> CongeMaternitePaterniteCalculator.compute(null, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void typeCongeNull_throws() {
        var input = new CongeMaternitePaterniteInput(
                null, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        assertThatThrownBy(() -> CongeMaternitePaterniteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("congé");
    }

    @Test
    void rangEnfantHorsPlage_throws() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 0, false, 3000.0,
                DATE_DEBUT, false, false);
        assertThatThrownBy(() -> CongeMaternitePaterniteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rang");
    }

    @Test
    void salaireNegatif_throws() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, -1.0,
                DATE_DEBUT, false, false);
        assertThatThrownBy(() -> CongeMaternitePaterniteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dateDebutNull_throws() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                null, false, false);
        assertThatThrownBy(() -> CongeMaternitePaterniteCalculator.compute(input, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ── AC10 : bases juridiques contiennent les fondements clés ────────────

    @Test
    void basesJuridiques_maternite_contiennentL1225() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.MATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1225-17") || b.contains("L. 1225-4"));
    }

    @Test
    void basesJuridiques_paternite_contiennentL1225_35() {
        var input = new CongeMaternitePaterniteInput(
                TypeConge.PATERNITE, 1, false, 3000.0,
                DATE_DEBUT, false, false);
        var r = CongeMaternitePaterniteCalculator.compute(input, "FRANCE");
        assertThat(r.basesJuridiques())
                .anyMatch(b -> b.contains("L. 1225-35"));
    }
}
