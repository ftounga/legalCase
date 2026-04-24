package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesFamilleCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_quasiAutomatique_10AnsAvecFamilleInsertionEtPasMenace_scoreMax() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2015, 1, 1),
                130, // > 10 ans
                true,
                2,
                7,
                true,
                false,
                LocalDate.of(2026, 4, 10),
                clockAt(today));

        assertThat(r.presence5AnsOk()).isTrue();
        assertThat(r.presence10AnsOk()).isTrue();
        assertThat(r.liensFamiliauxOk()).isTrue();
        assertThat(r.insertionOk()).isTrue();
        assertThat(r.pasMenace()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100); // 30+30+10+20+20
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateExpirationInstructionSiDemande()).isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(r.baseJuridique()).contains("L.435-1").contains("Valls");
        assertThat(r.formule()).contains("ELEVEE").contains("100/100");
    }

    @Test
    void compute_5Ans_conjoint_insertion_pasMenace_moyenne() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72, // 6 ans
                true,
                0,
                0,
                true,
                false,
                null,
                clockAt(today));

        // 20 (5ans) + 30 (liens conjoint) + 0 (pas de scolarité 5ans) + 20 (insertion) + 20 (pas menace) = 90
        assertThat(r.scoreGlobal()).isEqualTo(90);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_5AnsEnfantsScolarises5Ans_bonusApplique() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                false,
                1,
                5,
                true,
                false,
                null,
                clockAt(today));

        // 20 + 30 + 10 (bonus scolarité) + 20 + 20 = 100
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
    }

    @Test
    void compute_bonusScolarite_pasApplique_siScolariteMoins5Ans() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                false,
                1,
                4,
                true,
                false,
                null,
                clockAt(today));

        // 20 + 30 + 0 + 20 + 20 = 90
        assertThat(r.scoreGlobal()).isEqualTo(90);
    }

    @Test
    void compute_bonusScolarite_pasApplique_siLiensFamiliauxKo() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                false,
                0, // aucun enfant → liens KO
                10,
                true,
                false,
                null,
                clockAt(today));

        // 20 + 0 (liens KO, pas de bonus) + 20 + 20 = 60
        assertThat(r.liensFamiliauxOk()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(60);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_moinsDe5Ans_familleOk_insertionOk_pasMenace_moyenne() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2023, 1, 1),
                40, // < 5 ans
                true,
                0,
                0,
                true,
                false,
                null,
                clockAt(today));

        // 0 + 30 + 20 + 20 = 70
        assertThat(r.presence5AnsOk()).isFalse();
        assertThat(r.scoreGlobal()).isEqualTo(70);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
        assertThat(r.criteresNonRemplis()).contains("Présence en France inférieure à 5 ans (signal faible)");
    }

    @Test
    void compute_allKo_faible() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2025, 6, 1),
                10,
                false,
                0,
                0,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.scoreGlobal()).isEqualTo(0);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).hasSize(4);
    }

    @Test
    void compute_verdict_seuilsExacts() {
        LocalDate today = LocalDate.of(2026, 4, 24);

        // Score = 80 → ELEVEE (20 + 30 + 10 + 20 + 0 = 80, mais menace KO)
        AesFamilleResult rElevee = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                false,
                1,
                5,
                true,
                true, // menace → -20
                null,
                clockAt(today));
        assertThat(rElevee.scoreGlobal()).isEqualTo(80);
        assertThat(rElevee.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");

        // Score = 50 → MOYENNE (30 liens + 20 pas menace = 50)
        AesFamilleResult rMoyenne = AesFamilleCalculator.compute(
                LocalDate.of(2024, 1, 1),
                16,
                true,
                0,
                0,
                false,
                false,
                null,
                clockAt(today));
        assertThat(rMoyenne.scoreGlobal()).isEqualTo(50);
        assertThat(rMoyenne.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");

        // Score = 49 → FAIBLE (30 + 19 impossible ; on construit 30+0+0+20 = 50, donc tester 40)
        AesFamilleResult rFaible = AesFamilleCalculator.compute(
                LocalDate.of(2024, 1, 1),
                16,
                false,
                0,
                0,
                true, // +20
                false, // +20
                null,
                clockAt(today));
        assertThat(rFaible.scoreGlobal()).isEqualTo(40);
        assertThat(rFaible.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
    }

    @Test
    void compute_liensFamiliauxOk_siSeulConjoint() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                true,
                0,
                0,
                false,
                false,
                null,
                clockAt(today));
        assertThat(r.liensFamiliauxOk()).isTrue();
    }

    @Test
    void compute_liensFamiliauxOk_siSeulsEnfantsScolarises() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                false,
                1,
                2,
                false,
                false,
                null,
                clockAt(today));
        assertThat(r.liensFamiliauxOk()).isTrue();
    }

    @Test
    void compute_messagesIncluentValls() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2015, 1, 1),
                130,
                true,
                2,
                7,
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("faisceau d'indices"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Preuves à constituer"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("L.435-2"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("10 ans"));
    }

    @Test
    void compute_dateExpirationInstruction_nullIfNoDepot() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                true,
                0,
                0,
                true,
                false,
                null,
                clockAt(today));
        assertThat(r.dateExpirationInstructionSiDemande()).isNull();
    }

    @Test
    void compute_dateExpirationInstruction_depotPlus6Months() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult r = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                true,
                0,
                0,
                true,
                false,
                LocalDate.of(2026, 1, 15),
                clockAt(today));
        assertThat(r.dateExpirationInstructionSiDemande()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void compute_futureDateEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesFamilleCalculator.compute(
                LocalDate.of(2026, 5, 1),
                12,
                true,
                0,
                0,
                true,
                false,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullDateEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesFamilleCalculator.compute(
                null,
                12,
                true,
                0,
                0,
                true,
                false,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateEntreeFrance");
    }

    @Test
    void compute_dureePresenceNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                -1,
                true,
                0,
                0,
                true,
                false,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureePresenceMois");
    }

    @Test
    void compute_enfantsNegatifs_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                true,
                -1,
                0,
                true,
                false,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("enfantsScolarisesFrance");
    }

    @Test
    void compute_dureeScolariteNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1),
                72,
                true,
                1,
                -1,
                true,
                false,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeScolaritePlusAncienEnfantAnnees");
    }

    @Test
    void compute_dateDepotBeforeEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesFamilleCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                true,
                0,
                0,
                true,
                false,
                LocalDate.of(2021, 1, 1),
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }

    @Test
    void compute_pasMenace_equalsNegationMenaceOrdrePublic() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesFamilleResult rSafe = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1), 72, true, 0, 0, true, false, null, clockAt(today));
        assertThat(rSafe.pasMenace()).isTrue();
        AesFamilleResult rBad = AesFamilleCalculator.compute(
                LocalDate.of(2020, 4, 1), 72, true, 0, 0, true, true, null, clockAt(today));
        assertThat(rBad.pasMenace()).isFalse();
        assertThat(rBad.criteresNonRemplis()).contains("Menace ordre public présumée");
    }
}
