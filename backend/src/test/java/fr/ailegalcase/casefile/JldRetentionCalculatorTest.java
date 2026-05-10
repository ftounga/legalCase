package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-208-01 : tests unitaires de {@link JldRetentionCalculator}.
 */
class JldRetentionCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_nominal_disponible() {
        // notif 2026-05-08, today same day → expire 2026-05-10, reste 2 j → DISPONIBLE (>1)
        JldRetentionResult r = JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 8),
                "EXECUTION_OQTF",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 8)));

        assertThat(r.motifPlacement()).isEqualTo("EXECUTION_OQTF");
        assertThat(r.dateExpirationSaisineJld()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(r.dateAudienceJld()).isEqualTo(LocalDate.of(2026, 5, 13));
        assertThat(r.joursRestantsAvantSaisine()).isEqualTo(2L);
        assertThat(r.statut()).isEqualTo("DISPONIBLE");
        assertThat(r.dateExpirationRecoursAppel()).isNull();
        assertThat(r.baseJuridique()).contains("L.741-1").contains("L.743-9");
    }

    @Test
    void compute_urgent_when1DayLeft() {
        // notif 2026-05-08, today 2026-05-09 → reste 1 j → URGENT
        JldRetentionResult r = JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 8),
                "ITF",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 9)));

        assertThat(r.joursRestantsAvantSaisine()).isEqualTo(1L);
        assertThat(r.statut()).isEqualTo("URGENT");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("URGENT"));
    }

    @Test
    void compute_expire_pastDeadline() {
        // notif 2026-05-01, today 2026-05-08 → reste -5 → EXPIRE
        JldRetentionResult r = JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 1),
                "EXECUTION_OQTF",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 8)));

        assertThat(r.joursRestantsAvantSaisine()).isNegative();
        assertThat(r.statut()).isEqualTo("EXPIRE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("EXPIRÉ"));
    }

    @Test
    void compute_recoursForme_setsAppelDate() {
        // recours formé le 2026-05-09 → appel jusqu'au 2026-05-10
        JldRetentionResult r = JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 8),
                "EXECUTION_OQTF",
                true,
                LocalDate.of(2026, 5, 9),
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.statut()).isEqualTo("RECOURS_FORME");
        assertThat(r.joursRestantsAvantSaisine()).isZero();
        assertThat(r.dateExpirationRecoursAppel()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(r.formule()).contains("Appel");
    }

    @Test
    void compute_motifDublin_addsDublinMessage() {
        JldRetentionResult r = JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 8),
                "DUBLIN_TRANSFERT",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 8)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Dublin"));
    }

    @Test
    void compute_allMotifsAccepted() {
        for (String motif : java.util.List.of(
                "EXECUTION_OQTF", "ITF", "INTERDICTION_TERRITOIRE", "DUBLIN_TRANSFERT", "AUTRE")) {
            JldRetentionResult r = JldRetentionCalculator.compute(
                    LocalDate.of(2026, 5, 8),
                    motif,
                    false,
                    null,
                    clockAt(LocalDate.of(2026, 5, 8)));
            assertThat(r.motifPlacement()).isEqualTo(motif);
            assertThat(r.messages()).isNotEmpty();
        }
    }

    @Test
    void compute_futureNotification_throws() {
        assertThatThrownBy(() -> JldRetentionCalculator.compute(
                LocalDate.of(2026, 6, 1),
                "EXECUTION_OQTF",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_unknownMotif_throws() {
        assertThatThrownBy(() -> JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 8),
                "MOTIF_BIDON",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifPlacement");
    }

    @Test
    void compute_recoursFormeWithoutDateRecours_throws() {
        assertThatThrownBy(() -> JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 8),
                "EXECUTION_OQTF",
                true,
                null,
                clockAt(LocalDate.of(2026, 5, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_dateRecoursBeforeNotification_throws() {
        assertThatThrownBy(() -> JldRetentionCalculator.compute(
                LocalDate.of(2026, 5, 8),
                "EXECUTION_OQTF",
                true,
                LocalDate.of(2026, 5, 1),
                clockAt(LocalDate.of(2026, 5, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_nullNotification_throws() {
        assertThatThrownBy(() -> JldRetentionCalculator.compute(
                null,
                "EXECUTION_OQTF",
                false,
                null,
                clockAt(LocalDate.of(2026, 5, 8))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationPlacement");
    }
}
