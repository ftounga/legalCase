package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OqtfSansDelaiCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    /** Clock fixé pour stabilité des tests temporels (précision à la seconde). */
    private Clock clockAt(LocalDateTime now) {
        return Clock.fixed(now.atZone(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_risqueFuite_noRecours_disponible() {
        // notif 2026-04-02T14:30, now 2026-04-03T00:30 → expire 2026-04-04T14:30, reste 38h → DISPONIBLE
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "RISQUE_FUITE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.motifSansDelai()).isEqualTo("RISQUE_FUITE");
        assertThat(r.placementCra()).isFalse();
        assertThat(r.recoursForme()).isFalse();
        assertThat(r.dateHeureExpirationDelaiRecours())
                .isEqualTo(LocalDateTime.of(2026, 4, 4, 14, 30));
        assertThat(r.heuresRestantes()).isEqualTo(38L);
        assertThat(r.statutDelaiRecours()).isEqualTo("DISPONIBLE");
        assertThat(r.dateHeureAudiencePrevisionnelle()).isNull();
        assertThat(r.dateDecisionPrevisionnelle()).isNull();
        assertThat(r.refereDisponibles()).containsExactly("REFERE_LIBERTE_L521_2");
        assertThat(r.baseJuridique()).contains("L.731-1").contains("L.614-8").contains("R.776-26/27");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.731-1 1°"));
    }

    @Test
    void compute_urgentStatus_whenLessThan24Hours() {
        // notif 2026-04-02T14:30, now 2026-04-04T00:30 → expire 2026-04-04T14:30, reste 14h → URGENT
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "TROUBLE_ORDRE_PUBLIC",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 4, 0, 30)));

        assertThat(r.heuresRestantes()).isEqualTo(14L);
        assertThat(r.statutDelaiRecours()).isEqualTo("URGENT");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("URGENT"));
    }

    @Test
    void compute_urgentStatus_exactly24Hours() {
        // notif 2026-04-02T14:30, now 2026-04-03T14:30 → expire 2026-04-04T14:30, reste 24h → URGENT (seuil inclusif)
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "AUTRE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 14, 30)));

        assertThat(r.heuresRestantes()).isEqualTo(24L);
        assertThat(r.statutDelaiRecours()).isEqualTo("URGENT");
    }

    @Test
    void compute_disponible_25Hours() {
        // reste 25h → DISPONIBLE
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "AUTRE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 13, 30)));

        assertThat(r.heuresRestantes()).isEqualTo(25L);
        assertThat(r.statutDelaiRecours()).isEqualTo("DISPONIBLE");
    }

    @Test
    void compute_expire_whenPastDeadline() {
        // notif 2026-04-01T10:00, now 2026-04-04T12:00 → expire 2026-04-03T10:00, reste -50 → EXPIRE
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 1, 10, 0),
                "OQTF_PRECEDENTE_INEXECUTEE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 4, 12, 0)));

        assertThat(r.heuresRestantes()).isNegative();
        assertThat(r.statutDelaiRecours()).isEqualTo("EXPIRE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("EXPIRÉ"));
    }

    @Test
    void compute_recoursForme_setsAudienceAndDecisionDates() {
        // recours 2026-04-03T10:00 → audience +96h = 2026-04-07T10:00, décision +42j = 2026-05-15
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "RISQUE_FUITE",
                false,
                true,
                LocalDateTime.of(2026, 4, 3, 10, 0),
                clockAt(LocalDateTime.of(2026, 4, 4, 0, 0)));

        assertThat(r.statutDelaiRecours()).isEqualTo("RECOURS_FORME");
        assertThat(r.heuresRestantes()).isZero();
        assertThat(r.dateHeureAudiencePrevisionnelle())
                .isEqualTo(LocalDateTime.of(2026, 4, 7, 10, 0));
        assertThat(r.dateDecisionPrevisionnelle())
                .isEqualTo(java.time.LocalDate.of(2026, 5, 15));
        assertThat(r.formule()).contains("96h").contains("6 semaines");
    }

    @Test
    void compute_placementCra_addsControleJldAndMessage() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "RISQUE_FUITE",
                true,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.placementCra()).isTrue();
        assertThat(r.refereDisponibles())
                .containsExactly("REFERE_LIBERTE_L521_2", "CONTROLE_JLD_LIBERTE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.742-1"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CRA"));
    }

    @Test
    void compute_placementCraFalse_doesNotAddControleJld() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "RISQUE_FUITE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.refereDisponibles()).containsExactly("REFERE_LIBERTE_L521_2");
        assertThat(r.messages()).noneSatisfy(m -> assertThat(m).contains("L.742-1"));
    }

    @Test
    void compute_risqueFuiteMessage_citesL731_1_1() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "RISQUE_FUITE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.731-1 1°"));
    }

    @Test
    void compute_troubleOrdrePublicMessage_mentionsMenaceReelle() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "TROUBLE_ORDRE_PUBLIC",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("menace réelle"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.731-1 2°"));
    }

    @Test
    void compute_oqtfPrecedenteInexecuteeMessage_mentionsL731_1_3() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "OQTF_PRECEDENTE_INEXECUTEE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.731-1 3°"));
    }

    @Test
    void compute_autreMessage_mentionsVerifierMotif() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "AUTRE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Vérifier"));
    }

    @Test
    void compute_allMessages_includeCriticalDelayAndSuspensive() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "AUTRE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("48 heures"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.614-8"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("suspensif"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.521-2"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("R.776-26"));
    }

    @Test
    void compute_futureNotification_throws() {
        assertThatThrownBy(() -> OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 5, 1, 10, 0),
                "RISQUE_FUITE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 10, 10, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullMotif_throws() {
        assertThatThrownBy(() -> OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                null,
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifSansDelai");
    }

    @Test
    void compute_unknownMotif_throws() {
        assertThatThrownBy(() -> OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "MOTIF_INCONNU",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifSansDelai");
    }

    @Test
    void compute_recoursFormeWithoutDateRecours_throws() {
        assertThatThrownBy(() -> OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "RISQUE_FUITE",
                false,
                true,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateHeureRecours");
    }

    @Test
    void compute_dateRecoursBeforeNotification_throws() {
        assertThatThrownBy(() -> OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 10, 10, 0),
                "RISQUE_FUITE",
                false,
                true,
                LocalDateTime.of(2026, 4, 1, 10, 0),
                clockAt(LocalDateTime.of(2026, 4, 15, 10, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateHeureRecours");
    }

    @Test
    void compute_nullDateNotification_throws() {
        assertThatThrownBy(() -> OqtfSansDelaiCalculator.compute(
                null,
                "RISQUE_FUITE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 10, 10, 0))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateHeureNotificationOqtf");
    }

    @Test
    void compute_allFourMotifsAccepted() {
        for (String motif : java.util.List.of(
                "RISQUE_FUITE", "TROUBLE_ORDRE_PUBLIC", "OQTF_PRECEDENTE_INEXECUTEE", "AUTRE")) {
            OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                    LocalDateTime.of(2026, 4, 2, 14, 30),
                    motif,
                    false,
                    false,
                    null,
                    clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));
            assertThat(r.motifSansDelai()).isEqualTo(motif);
            assertThat(r.messages()).isNotEmpty();
        }
    }

    @Test
    void compute_expirationEqualsNotificationPlus48Hours() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "AUTRE",
                false,
                false,
                null,
                clockAt(LocalDateTime.of(2026, 4, 3, 0, 30)));

        assertThat(r.dateHeureExpirationDelaiRecours())
                .isEqualTo(LocalDateTime.of(2026, 4, 4, 14, 30));
    }

    @Test
    void compute_recoursFormeWithCra_bothReferesAndCraMessage() {
        OqtfSansDelaiResult r = OqtfSansDelaiCalculator.compute(
                LocalDateTime.of(2026, 4, 2, 14, 30),
                "TROUBLE_ORDRE_PUBLIC",
                true,
                true,
                LocalDateTime.of(2026, 4, 3, 10, 0),
                clockAt(LocalDateTime.of(2026, 4, 4, 0, 0)));

        assertThat(r.statutDelaiRecours()).isEqualTo("RECOURS_FORME");
        assertThat(r.refereDisponibles())
                .containsExactly("REFERE_LIBERTE_L521_2", "CONTROLE_JLD_LIBERTE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CRA"));
        assertThat(r.formule()).contains("CRA");
    }
}
