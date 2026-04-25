package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReferesAdminCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    private static final LocalDate NOTIF_RECENTE = LocalDate.of(2026, 4, 10);
    private static final LocalDate TODAY = LocalDate.of(2026, 4, 20);

    @Test
    void compute_suspension_allConditions_returnsScore100() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, false, true,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteSuspension()).isEqualTo(100);
        assertThat(r.conditionsCumulativesL521_1Ok()).isTrue();
        assertThat(r.conditionsCumulativesL521_2Ok()).isFalse();
        assertThat(r.verdictRecommandation()).isEqualTo("REFERE_SUSPENSION_PRIORITAIRE");
        assertThat(r.delaiJugeTaJoursL521_1()).isEqualTo(30);
        assertThat(r.delaiJugeTaHeuresL521_2()).isEqualTo(48);
    }

    @Test
    void compute_suspension_singleCondition_returnsScore50() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, false, false,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteSuspension()).isEqualTo(50);
        assertThat(r.conditionsCumulativesL521_1Ok()).isFalse();
    }

    @Test
    void compute_noConditions_returnsScore0_andAucunProbable() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                false, false, false,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteSuspension()).isZero();
        assertThat(r.scoreSuccessProbabiliteLiberte()).isZero();
        assertThat(r.verdictRecommandation()).isEqualTo("AUCUN_PROBABLE");
    }

    @Test
    void compute_liberte_libertePrioritaire_whenScoreLiberteAhead() {
        // Liberté 100 vs suspension 50 → diff 50 ≥ 15 → LIBERTE_PRIO
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "LIBERTE", "IRTF", NOTIF_RECENTE,
                true, true, false,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteLiberte()).isEqualTo(100);
        assertThat(r.scoreSuccessProbabiliteSuspension()).isEqualTo(50);
        assertThat(r.conditionsCumulativesL521_2Ok()).isTrue();
        assertThat(r.verdictRecommandation()).isEqualTo("REFERE_LIBERTE_PRIORITAIRE");
    }

    @Test
    void compute_lesDeux_returnsLesDeuxCumules_whenBothConditionsOk() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "LES_DEUX", "OQTF", NOTIF_RECENTE,
                true, true, true,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteSuspension()).isEqualTo(100);
        assertThat(r.scoreSuccessProbabiliteLiberte()).isEqualTo(100);
        assertThat(r.conditionsCumulativesL521_1Ok()).isTrue();
        assertThat(r.conditionsCumulativesL521_2Ok()).isTrue();
        assertThat(r.verdictRecommandation()).isEqualTo("LES_DEUX_CUMULES");
    }

    @Test
    void compute_preuvesUrgence_addBonus5PointsEach() {
        // Base suspension = 50 (urgence seul), 2 preuves = +10, score = 60
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, false, false,
                List.of("MENACE_VIE_PRIVEE", "TRANSFERT_IMMINENT"), false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteSuspension()).isEqualTo(60);
        assertThat(r.preuvesUrgence()).hasSize(2);
    }

    @Test
    void compute_preuvesUrgence_bonusCappedAt30() {
        // 6 preuves * 5 = 30 (capped). Base 50 → 80.
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, false, false,
                List.of("MENACE_VIE_PRIVEE", "TRANSFERT_IMMINENT", "IMPACT_SCOLARITE_ENFANTS",
                        "RUPTURE_SOINS_MEDICAUX", "RISQUE_VIOLENCES_PAYS_ORIGINE", "AUTRE"),
                false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteSuspension()).isEqualTo(80);
    }

    @Test
    void compute_scoreCappedAt100_withBaseAndBonus() {
        // Base 100 (les 2 conditions) + bonus 30 → cap à 100
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, false, true,
                List.of("MENACE_VIE_PRIVEE", "TRANSFERT_IMMINENT", "IMPACT_SCOLARITE_ENFANTS",
                        "RUPTURE_SOINS_MEDICAUX", "RISQUE_VIOLENCES_PAYS_ORIGINE", "AUTRE"),
                false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteSuspension()).isEqualTo(100);
    }

    @Test
    void compute_libertePrioritaire_withExactly15PointsAhead() {
        // Suspension: urgence=true + doutes=false + 0 preuves = 50 (1 cond)
        // Liberté: atteinte=true + urgence=true = 100 → diff 50 ≥ 15 → LIBERTE_PRIO
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "LIBERTE", "IRTF", NOTIF_RECENTE,
                true, true, false,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.scoreSuccessProbabiliteLiberte() - r.scoreSuccessProbabiliteSuspension())
                .isGreaterThanOrEqualTo(15);
        assertThat(r.verdictRecommandation()).isEqualTo("REFERE_LIBERTE_PRIORITAIRE");
    }

    @Test
    void compute_suspensionPrio_whenScoreEqual() {
        // Liberté 50 vs suspension 50 → pas de gap 15 → SUSPENSION_PRIO par défaut
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, false, false,
                List.of(), false,
                clockAt(TODAY));

        // Suspension 50 (urgence seul), Liberté 50 (urgence seul) → SUSPENSION_PRIO (pas LIBERTE_PRIO)
        assertThat(r.scoreSuccessProbabiliteSuspension())
                .isEqualTo(r.scoreSuccessProbabiliteLiberte());
        assertThat(r.verdictRecommandation()).isEqualTo("REFERE_SUSPENSION_PRIORITAIRE");
    }

    @Test
    void compute_formuleContainsScoresAndVerdict() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, false, true,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.formule()).contains("L.521-1").contains("L.521-2")
                .contains("100").contains("REFERE_SUSPENSION_PRIORITAIRE");
        assertThat(r.baseJuridique()).contains("L.521-1").contains("L.521-2").contains("CJA");
    }

    @Test
    void compute_messagesContainKeyReminders() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "LES_DEUX", "OQTF", NOTIF_RECENTE,
                true, true, true,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("48h"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("30 jours"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.521-1"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.521-2"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("cumuler"));
    }

    @Test
    void compute_demandeurDejaPrived_addsRetentionMessage() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "LIBERTE", "OQTF", NOTIF_RECENTE,
                true, true, false,
                List.of(), true,
                clockAt(TODAY));

        assertThat(r.demandeurDejaPrived()).isTrue();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("rétention"));
    }

    @Test
    void compute_decisionIRTF_addsLibertePrivateMessage() {
        ReferesAdminResult r = ReferesAdminCalculator.compute(
                "LIBERTE", "IRTF", NOTIF_RECENTE,
                true, true, false,
                List.of(), false,
                clockAt(TODAY));

        assertThat(r.decisionContestee()).isEqualTo("IRTF");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("IRTF"));
    }

    @Test
    void compute_nullTypeRefere_throws() {
        assertThatThrownBy(() -> ReferesAdminCalculator.compute(
                null, "OQTF", NOTIF_RECENTE,
                true, true, true, List.of(), false,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRefere");
    }

    @Test
    void compute_unknownTypeRefere_throws() {
        assertThatThrownBy(() -> ReferesAdminCalculator.compute(
                "INCONNU", "OQTF", NOTIF_RECENTE,
                true, true, true, List.of(), false,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRefere");
    }

    @Test
    void compute_unknownDecisionContestee_throws() {
        assertThatThrownBy(() -> ReferesAdminCalculator.compute(
                "SUSPENSION", "DECISION_BIDON", NOTIF_RECENTE,
                true, true, true, List.of(), false,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decisionContestee");
    }

    @Test
    void compute_nullDateNotification_throws() {
        assertThatThrownBy(() -> ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", null,
                true, true, true, List.of(), false,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationDecision");
    }

    @Test
    void compute_futureDateNotification_throws() {
        assertThatThrownBy(() -> ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", LocalDate.of(2026, 5, 1),
                true, true, true, List.of(), false,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_unknownPreuveUrgence_throws() {
        assertThatThrownBy(() -> ReferesAdminCalculator.compute(
                "SUSPENSION", "OQTF", NOTIF_RECENTE,
                true, true, true,
                List.of("PREUVE_BIDON"), false,
                clockAt(TODAY)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("preuveUrgence");
    }

    @Test
    void compute_allDecisionsAccepted() {
        for (String decision : List.of("OQTF", "RETRAIT_TITRE", "REFUS_TITRE", "IRTF", "AUTRE_MESURE_ADMIN")) {
            ReferesAdminResult r = ReferesAdminCalculator.compute(
                    "SUSPENSION", decision, NOTIF_RECENTE,
                    true, false, true, List.of(), false,
                    clockAt(TODAY));
            assertThat(r.decisionContestee()).isEqualTo(decision);
            assertThat(r.messages()).isNotEmpty();
        }
    }
}
