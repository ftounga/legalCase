package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesEtudiantCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_allConditionsMet_returnsVerdictEleveeScore100() {
        // today = 2026-04-24, entrée = 2022-04-01 (>3 ans) → presence OK
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                LocalDate.of(2026, 4, 10),
                clockAt(today));

        assertThat(r.presence3AnsOk()).isTrue();
        assertThat(r.scolarite2AnsConsecutivesOk()).isTrue();
        assertThat(r.resultatsAcceptables()).isTrue();
        assertThat(r.inscriptionValide()).isTrue();
        assertThat(r.moyensOk()).isTrue();
        assertThat(r.pasMenace()).isTrue();
        assertThat(r.parcoursCoherentOk()).isTrue();
        assertThat(r.scoreGlobal()).isEqualTo(100);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("ELEVEE");
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateExpirationInstructionSiDemande())
                .isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(r.baseJuridique())
                .contains("Valls").contains("L.412-1");
        assertThat(r.formule())
                .contains("tous critères remplis").contains("ELEVEE");
    }

    @Test
    void compute_presence3Ans_exactBoundary_inclusive() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2023, 4, 24),
                36,
                3,
                "BAC_PLUS_1_2",
                "MOYEN_PASSABLE",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.presence3AnsOk()).isTrue();
    }

    @Test
    void compute_presenceLessThan3Ans_butDureeMoisCovers_stillOk() {
        // Entrée récente (2 ans) mais dureePresenceMois >= 36 (cumul plusieurs séjours)
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2024, 4, 1),
                40,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.presence3AnsOk()).isTrue();
    }

    @Test
    void compute_presenceNotMet_addsToCriteresNonRemplis() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2024, 10, 1),
                18,
                2,
                "BAC_PLUS_1_2",
                "MOYEN_PASSABLE",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.presence3AnsOk()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("présence"));
    }

    @Test
    void compute_scolarite2Ans_exactBoundary() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                2,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.scolarite2AnsConsecutivesOk()).isTrue();
    }

    @Test
    void compute_scolarite1An_notOk() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                1,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.scolarite2AnsConsecutivesOk()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("2 années consécutives"));
    }

    @Test
    void compute_resultatsDifficultesRepetees_addsHintMessage() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "DIFFICULTES_REPETEES",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.resultatsAcceptables()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("Résultats"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("réorientation"));
    }

    @Test
    void compute_niveauBacPlus3_suggestsVlsTsChangement() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_3_4",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("VLS-TS"));
    }

    @Test
    void compute_niveauBacPlus5_suggestsVlsTsChangement() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_5_PLUS",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("VLS-TS"));
    }

    @Test
    void compute_niveauLycee_warnsAgeAndFamille() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "LYCEE",
                "MOYEN_PASSABLE",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("18 ans"));
    }

    @Test
    void compute_menaceOrdrePublic_blocks() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                true,
                true,
                null,
                clockAt(today));

        assertThat(r.pasMenace()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("Menace ordre public"));
    }

    @Test
    void compute_moyensKo_addsToCriteres() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                false,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.moyensOk()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("moyens"));
    }

    @Test
    void compute_parcoursIncoherent_addsToCriteres() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                false,
                null,
                clockAt(today));

        assertThat(r.parcoursCoherentOk()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("Parcours"));
    }

    @Test
    void compute_inscriptionKo_addsToCriteres() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                false,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.inscriptionValide()).isFalse();
        assertThat(r.criteresNonRemplis())
                .anyMatch(c -> c.contains("inscription"));
    }

    @Test
    void compute_verdictFaible_suggestsOtherRegimes() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2024, 10, 1),
                18,
                1,
                "BAC_PLUS_1_2",
                "DIFFICULTES_REPETEES",
                false,
                false,
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.scoreGlobal()).isLessThan(50);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("FAIBLE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("L.435-2"));
    }

    @Test
    void compute_verdictMoyenne_between50And79() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        // 4 critères majeurs OK (60 pts) + ordre public OK (10) = 70
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                false,
                false,
                false,
                null,
                clockAt(today));

        assertThat(r.scoreGlobal()).isBetween(50, 79);
        assertThat(r.verdictProbabiliteAcceptation()).isEqualTo("MOYENNE");
    }

    @Test
    void compute_dateExpirationInstruction_nullIfNoDepot() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.dateExpirationInstructionSiDemande()).isNull();
    }

    @Test
    void compute_dateExpirationInstruction_depotPlus6Months() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                LocalDate.of(2026, 1, 15),
                clockAt(today));

        assertThat(r.dateExpirationInstructionSiDemande())
                .isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void compute_messages_alwaysIncludeFaisceauIndices() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesEtudiantResult r = AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("faisceau d'indices"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("certificats de scolarité"));
    }

    @Test
    void compute_futureDateEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesEtudiantCalculator.compute(
                LocalDate.of(2026, 5, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullDateEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesEtudiantCalculator.compute(
                null,
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateEntreeFrance");
    }

    @Test
    void compute_dureeMoisNegative_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                -1,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureePresenceMois");
    }

    @Test
    void compute_anneesScolariteOutOfRange_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                25,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("anneesScolariteEnFranceConsecutives");
    }

    @Test
    void compute_niveauInvalide_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "POSTDOC",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("niveauEtudesActuel");
    }

    @Test
    void compute_resultatsInvalide_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "BOF",
                true,
                true,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("resultatsAcademiques");
    }

    @Test
    void compute_dateDepotBeforeEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesEtudiantCalculator.compute(
                LocalDate.of(2022, 4, 1),
                48,
                3,
                "BAC_PLUS_1_2",
                "EXCELLENT",
                true,
                true,
                false,
                true,
                LocalDate.of(2021, 1, 1),
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }
}
