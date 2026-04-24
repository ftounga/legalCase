package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DivorceAlterationCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_conditionsReunies_separation2ans_preuves_pasReconciliation_mariage10ans_elevee() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 4, 24), // 2 ans pile
                true,  // preuves documentaires
                false, // pas de tentative de reconciliation
                10,    // mariage 10 ans
                new BigDecimal("60000"),
                new BigDecimal("30000"),
                false,
                LocalDate.of(2026, 4, 24),
                clockAt(today));

        assertThat(r.dureeSeparationAnnees()).isEqualTo(2.0);
        assertThat(r.delaiObjectifOk()).isTrue();
        assertThat(r.absencePreuveReconciliation()).isTrue();
        assertThat(r.conditionsReunies()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100); // 40+25+20+15
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.baseJuridique()).contains("237").contains("238");
        assertThat(r.formule()).contains("ELEVEE").contains("100/100");
        // Prestation : |60k-30k| × (0.3 × 10) × 0.5 = 30000 × 3 × 0.5 = 45000 / 30000 × 3 × 1.5 = 135000
        assertThat(r.prestationCompensatoireFourchetteMin()).isEqualByComparingTo("45000.00");
        assertThat(r.prestationCompensatoireFourchetteMax()).isEqualByComparingTo("135000.00");
    }

    @Test
    void compute_separationMoinsDe1an_delaiKo_faible() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2025, 11, 1), // ~0.5 an
                true, false, 10,
                new BigDecimal("50000"),
                new BigDecimal("40000"),
                false,
                null,
                clockAt(today));

        assertThat(r.delaiObjectifOk()).isFalse();
        assertThat(r.conditionsReunies()).isFalse();
        // 0 (delai KO) + 25 + 20 + 15 = 60
        assertThat(r.scoreGlobal()).isEqualTo(60);
        assertThat(r.verdictProbabilite()).isEqualTo("MOYENNE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("1 an"));
    }

    @Test
    void compute_delaiApprecieDateAssignation_pasAujourdhui() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        // Cessation 2024-01-01, assignation 2024-12-01 → 0.92 an (< 1 an)
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 6,
                null, null, false,
                LocalDate.of(2024, 12, 1),
                clockAt(today));
        assertThat(r.delaiObjectifOk()).isFalse();
        assertThat(r.dureeSeparationAnnees()).isLessThan(1.0);
    }

    @Test
    void compute_tentativesReconciliation_absenceKo() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2023, 1, 1), // > 1 an
                true,
                true, // tentative de reconciliation
                10,
                null, null, false, null,
                clockAt(today));
        assertThat(r.absencePreuveReconciliation()).isFalse();
        assertThat(r.conditionsReunies()).isFalse();
        // 40 + 25 + 0 + 15 = 80
        assertThat(r.scoreGlobal()).isEqualTo(80);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("réconciliation"));
    }

    @Test
    void compute_pasPreuves_conditionsKo() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                false, // pas de preuves
                false, 10, null, null, false, null,
                clockAt(today));
        assertThat(r.conditionsReunies()).isFalse();
        // 40 + 0 + 20 + 15 = 75
        assertThat(r.scoreGlobal()).isEqualTo(75);
        assertThat(r.verdictProbabilite()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Preuves"));
    }

    @Test
    void compute_bonusDureeMariage_applique_a5ans() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false,
                5, // pile 5 ans → bonus
                null, null, false, null,
                clockAt(today));
        // 40 + 25 + 20 + 15 = 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
    }

    @Test
    void compute_bonusDureeMariage_pasApplique_si4ans() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 4,
                null, null, false, null,
                clockAt(today));
        // 40 + 25 + 20 + 0 = 85
        assertThat(r.scoreGlobal()).isEqualTo(85);
    }

    @Test
    void compute_allKo_scoreZero_faible() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2026, 1, 1), // < 1 an
                false, // pas de preuves
                true,  // tentative reconciliation
                3,     // < 5 ans
                null, null, false, null,
                clockAt(today));
        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictProbabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).hasSize(3);
        assertThat(r.conditionsReunies()).isFalse();
    }

    @Test
    void compute_verdict_seuils() {
        LocalDate today = LocalDate.of(2026, 4, 24);

        // Score 75 (ELEVEE) : delai 40 + preuves 25 + mariage 15 = 80 → > 75 ELEVEE
        DivorceAlterationResult rElevee = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1), true, true, 10, null, null, false, null, clockAt(today));
        assertThat(rElevee.scoreGlobal()).isEqualTo(80);
        assertThat(rElevee.verdictProbabilite()).isEqualTo("ELEVEE");

        // Score 40 (MOYENNE) : delai 40 seul
        DivorceAlterationResult rMoyenne = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1), false, true, 3, null, null, false, null, clockAt(today));
        assertThat(rMoyenne.scoreGlobal()).isEqualTo(40);
        assertThat(rMoyenne.verdictProbabilite()).isEqualTo("MOYENNE");

        // Score 35 (FAIBLE) : preuves 25 + mariage 15 = 40 (MOYENNE). Test 25 uniquement.
        DivorceAlterationResult rFaible = DivorceAlterationCalculator.compute(
                LocalDate.of(2026, 1, 1), true, true, 3, null, null, false, null, clockAt(today));
        assertThat(rFaible.scoreGlobal()).isEqualTo(25);
        assertThat(rFaible.verdictProbabilite()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_prestationCompensatoire_facteurPlafonnea10() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        // Mariage 25 ans → facteur plafonné à 10
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 25,
                new BigDecimal("80000"),
                new BigDecimal("20000"),
                true,
                null,
                clockAt(today));
        // diff = 60000, facteur = 0.3 * 10 = 3, base = 180000
        // min = 180000 * 0.5 = 90000, max = 180000 * 1.5 = 270000
        assertThat(r.prestationCompensatoireFourchetteMin()).isEqualByComparingTo("90000.00");
        assertThat(r.prestationCompensatoireFourchetteMax()).isEqualByComparingTo("270000.00");
    }

    @Test
    void compute_prestationCompensatoire_revenusEgaux_fourchetteZero() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 10,
                new BigDecimal("50000"),
                new BigDecimal("50000"),
                false,
                null,
                clockAt(today));
        assertThat(r.prestationCompensatoireFourchetteMin()).isEqualByComparingTo("0.00");
        assertThat(r.prestationCompensatoireFourchetteMax()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_prestationCompensatoire_revenusNuls_fourchetteZero() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 10,
                null, null, false, null,
                clockAt(today));
        assertThat(r.prestationCompensatoireFourchetteMin()).isEqualByComparingTo("0.00");
        assertThat(r.prestationCompensatoireFourchetteMax()).isEqualByComparingTo("0.00");
    }

    @Test
    void compute_patrimoineCommun_messageAjoute() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 10, null, null,
                true, // patrimoine commun significatif
                null,
                clockAt(today));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Patrimoine commun"));
    }

    @Test
    void compute_messages_incluentBases() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        DivorceAlterationResult r = DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 10, null, null, false, null,
                clockAt(today));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("art. 266"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("art. 270"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("F-FA-12"));
    }

    @Test
    void compute_futureCessation_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAlterationCalculator.compute(
                LocalDate.of(2027, 1, 1),
                true, false, 10, null, null, false, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullCessation_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAlterationCalculator.compute(
                null, true, false, 10, null, null, false, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateCessationVieCommune");
    }

    @Test
    void compute_dureeMariageNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, -1, null, null, false, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeMariageAnnees");
    }

    @Test
    void compute_revenusNegatifs_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 1, 1),
                true, false, 10,
                new BigDecimal("-100"), null, false, null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("revenusAnnuelsEpoux1Eur");
    }

    @Test
    void compute_dateAssignationAvantCessation_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> DivorceAlterationCalculator.compute(
                LocalDate.of(2024, 6, 1),
                true, false, 10, null, null, false,
                LocalDate.of(2024, 1, 1),
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAssignation");
    }
}
