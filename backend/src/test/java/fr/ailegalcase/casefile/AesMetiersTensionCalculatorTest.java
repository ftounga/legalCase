package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesMetiersTensionCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_allConditionsMet_returnsConditionsReunies() {
        // today = 2026-04-24, entrée = 2022-04-01 (>3 ans) → presence3Ans OK
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                18,
                true,
                "N1101",
                false,
                true,
                LocalDate.of(2026, 4, 10),
                clockAt(today));

        assertThat(r.presence3Ans()).isTrue();
        assertThat(r.activite12MoisOk()).isTrue();
        assertThat(r.conditionsReunies()).isTrue();
        assertThat(r.criteresNonRemplis()).isEmpty();
        assertThat(r.dateEligibiliteAtteinte()).isNull();
        assertThat(r.dateExpirationInstructionSiDemande())
                .isEqualTo(LocalDate.of(2026, 10, 10));
        assertThat(r.baseJuridique()).contains("L.435-4").contains("26/01/2024");
        assertThat(r.formule()).contains("conditions réunies").contains("N1101");
    }

    @Test
    void compute_presence3Ans_exactBoundary_inclusive() {
        // entrée exactement 3 ans avant today → presence3Ans = true
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2023, 4, 24),
                15,
                true,
                "N1101",
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.presence3Ans()).isTrue();
        assertThat(r.dateEligibiliteAtteinte()).isNull();
    }

    @Test
    void compute_presence3Ans_notMet_setsDateEligibilite() {
        // entrée 2024-04-01, today 2026-04-24 → ~2 ans → presence KO
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2024, 4, 1),
                18,
                true,
                "N1101",
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.presence3Ans()).isFalse();
        assertThat(r.conditionsReunies()).isFalse();
        assertThat(r.criteresNonRemplis()).contains("Moins de 3 ans de présence en France");
        assertThat(r.dateEligibiliteAtteinte()).isEqualTo(LocalDate.of(2027, 4, 1));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("3 ans atteinte"));
    }

    @Test
    void compute_activite12MoisOk_exactlyTwelve() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                12,
                true,
                "N1101",
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.activite12MoisOk()).isTrue();
        assertThat(r.conditionsReunies()).isTrue();
    }

    @Test
    void compute_activite11Mois_notOk() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                11,
                true,
                "N1101",
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.activite12MoisOk()).isFalse();
        assertThat(r.conditionsReunies()).isFalse();
        assertThat(r.criteresNonRemplis())
                .contains("Moins de 12 mois d'activité salariée dans les 24 derniers mois");
    }

    @Test
    void compute_metierPasEnTension_addsHint() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                18,
                false,
                null,
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.conditionsReunies()).isFalse();
        assertThat(r.criteresNonRemplis()).contains("Métier ne figurant pas sur liste tension");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("arrêté consolidé"));
    }

    @Test
    void compute_menaceOrdrePublic_blocks() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                18,
                true,
                "N1101",
                true,
                true,
                null,
                clockAt(today));

        assertThat(r.conditionsReunies()).isFalse();
        assertThat(r.criteresNonRemplis()).contains("Menace ordre public présumée");
    }

    @Test
    void compute_contratInvalide_blocks() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                18,
                true,
                "N1101",
                false,
                false,
                null,
                clockAt(today));

        assertThat(r.conditionsReunies()).isFalse();
        assertThat(r.criteresNonRemplis()).contains("Pas de contrat ou promesse valide");
    }

    @Test
    void compute_allCriteresKo_listsAll() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2025, 4, 1),
                5,
                false,
                null,
                true,
                false,
                null,
                clockAt(today));

        assertThat(r.conditionsReunies()).isFalse();
        assertThat(r.criteresNonRemplis()).hasSize(5);
    }

    @Test
    void compute_dateExpirationInstruction_nullIfNoDepot() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                18,
                true,
                "N1101",
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.dateExpirationInstructionSiDemande()).isNull();
    }

    @Test
    void compute_dateExpirationInstruction_depotPlus6Months() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                18,
                true,
                "N1101",
                false,
                true,
                LocalDate.of(2026, 1, 15),
                clockAt(today));

        assertThat(r.dateExpirationInstructionSiDemande()).isEqualTo(LocalDate.of(2026, 7, 15));
    }

    @Test
    void compute_messages_alwaysIncludeExperimentale() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        AesMetiersTensionResult r = AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                18,
                true,
                "N1101",
                false,
                true,
                null,
                clockAt(today));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("expérimentale"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("bulletins de paie"));
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("F-IM-09-02/03/04"));
    }

    @Test
    void compute_futureDateEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesMetiersTensionCalculator.compute(
                LocalDate.of(2026, 5, 1),
                12,
                true,
                null,
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
        assertThatThrownBy(() -> AesMetiersTensionCalculator.compute(
                null,
                12,
                true,
                null,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateEntreeFrance");
    }

    @Test
    void compute_moisNegatif_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                -1,
                true,
                null,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moisActiviteSalarieeDernieres24Mois");
    }

    @Test
    void compute_moisSup24_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                25,
                true,
                null,
                false,
                true,
                null,
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("moisActiviteSalarieeDernieres24Mois");
    }

    @Test
    void compute_dateDepotBeforeEntree_throws() {
        LocalDate today = LocalDate.of(2026, 4, 24);
        assertThatThrownBy(() -> AesMetiersTensionCalculator.compute(
                LocalDate.of(2022, 4, 1),
                12,
                true,
                null,
                false,
                true,
                LocalDate.of(2021, 1, 1),
                clockAt(today)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDepotDemande");
    }
}
