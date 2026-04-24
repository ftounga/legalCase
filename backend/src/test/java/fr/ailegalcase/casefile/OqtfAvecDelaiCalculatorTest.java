package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OqtfAvecDelaiCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");

    /** Clock fixé au 2026-04-10 pour stabilité des tests temporels. */
    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    @Test
    void compute_refusTitre_noRecours_disponible() {
        // notif 2026-04-01, today 2026-04-10 → expire 2026-05-01, reste 21 jours → DISPONIBLE
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "REFUS_TITRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.motifOqtf()).isEqualTo("REFUS_TITRE");
        assertThat(r.recoursForme()).isFalse();
        assertThat(r.dateExpirationDdv()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(r.dateExpirationDelaiRecours()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(r.joursRestantsAvantExpirationDelai()).isEqualTo(21);
        assertThat(r.statutDelaiRecours()).isEqualTo("DISPONIBLE");
        assertThat(r.dateAudiencePrevisionnelle()).isNull();
        assertThat(r.dateDecisionTaPrevisionnelle()).isNull();
        assertThat(r.referedDisponibles()).containsExactly(
                "REFERE_SUSPENSION_L521_1", "REFERE_LIBERTE_L521_2");
        assertThat(r.baseJuridique()).contains("L.614-5").contains("L.614-6").contains("R.776-18");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m)
                .contains("refus de titre"));
    }

    @Test
    void compute_urgentStatus_whenLessOrEqual7Days() {
        // notif 2026-04-01, today 2026-04-26 → expire 2026-05-01, reste 5 jours → URGENT
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "SEJOUR_IRREGULIER",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 26)));

        assertThat(r.joursRestantsAvantExpirationDelai()).isEqualTo(5);
        assertThat(r.statutDelaiRecours()).isEqualTo("URGENT");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("URGENT"));
    }

    @Test
    void compute_urgentStatus_exactly7Days() {
        // notif 2026-04-01, today 2026-04-24 → reste 7 jours → URGENT (seuil inclusif)
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "AUTRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 24)));

        assertThat(r.joursRestantsAvantExpirationDelai()).isEqualTo(7);
        assertThat(r.statutDelaiRecours()).isEqualTo("URGENT");
    }

    @Test
    void compute_disponible_8Days() {
        // reste 8 jours → DISPONIBLE
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "AUTRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 23)));

        assertThat(r.joursRestantsAvantExpirationDelai()).isEqualTo(8);
        assertThat(r.statutDelaiRecours()).isEqualTo("DISPONIBLE");
    }

    @Test
    void compute_expire_whenPastDeadline() {
        // notif 2026-03-01, today 2026-04-10 → expire 2026-03-31, reste -10 → EXPIRE
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 3, 1),
                "EXPIRATION_TITRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.joursRestantsAvantExpirationDelai()).isNegative();
        assertThat(r.statutDelaiRecours()).isEqualTo("EXPIRE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("EXPIRÉ"));
    }

    @Test
    void compute_recoursForme_setsAudienceAndDecisionDates() {
        // recours formé le 2026-04-15 → audience J+90 = 2026-07-14, décision J+180 = 2026-10-12
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "RETRAIT_TITRE",
                true,
                LocalDate.of(2026, 4, 15),
                clockAt(LocalDate.of(2026, 4, 20)));

        assertThat(r.statutDelaiRecours()).isEqualTo("RECOURS_FORME");
        assertThat(r.joursRestantsAvantExpirationDelai()).isZero();
        assertThat(r.dateAudiencePrevisionnelle()).isEqualTo(LocalDate.of(2026, 7, 14));
        assertThat(r.dateDecisionTaPrevisionnelle()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(r.formule()).contains("J+90").contains("J+180");
    }

    @Test
    void compute_refusTitreMessage_includesL614_6() {
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "REFUS_TITRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("L.614-6"));
    }

    @Test
    void compute_sejourIrregulierMessage_mentionsOrderPublic() {
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "SEJOUR_IRREGULIER",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("ordre public"));
    }

    @Test
    void compute_expirationTitreMessage_mentionsRenouvellement() {
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "EXPIRATION_TITRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("renouvellement"));
    }

    @Test
    void compute_retraitTitreMessage_mentionsRetrait() {
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "RETRAIT_TITRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("retrait"));
    }

    @Test
    void compute_allMessages_includeSuspensiveAndReferences() {
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "AUTRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("suspensif"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.521-1"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("L.521-2"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("jours francs"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("interprète"));
    }

    @Test
    void compute_futureNotification_throws() {
        assertThatThrownBy(() -> OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 5, 1),
                "REFUS_TITRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_nullMotif_throws() {
        assertThatThrownBy(() -> OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                null,
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifOqtf");
    }

    @Test
    void compute_unknownMotif_throws() {
        assertThatThrownBy(() -> OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "MOTIF_INCONNU",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifOqtf");
    }

    @Test
    void compute_recoursFormeWithoutDateRecours_throws() {
        assertThatThrownBy(() -> OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "REFUS_TITRE",
                true,
                null,
                clockAt(LocalDate.of(2026, 4, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_dateRecoursBeforeNotification_throws() {
        assertThatThrownBy(() -> OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 10),
                "REFUS_TITRE",
                true,
                LocalDate.of(2026, 4, 1),
                clockAt(LocalDate.of(2026, 4, 15))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_nullDateNotification_throws() {
        assertThatThrownBy(() -> OqtfAvecDelaiCalculator.compute(
                null,
                "REFUS_TITRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationOqtf");
    }

    @Test
    void compute_allFiveMotifsAccepted() {
        for (String motif : java.util.List.of(
                "REFUS_TITRE", "EXPIRATION_TITRE", "SEJOUR_IRREGULIER", "RETRAIT_TITRE", "AUTRE")) {
            OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                    LocalDate.of(2026, 4, 1),
                    motif,
                    false,
                    null,
                    clockAt(LocalDate.of(2026, 4, 10)));
            assertThat(r.motifOqtf()).isEqualTo(motif);
            assertThat(r.messages()).isNotEmpty();
        }
    }

    @Test
    void compute_ddvEqualsNotificationPlus30Days() {
        OqtfAvecDelaiResult r = OqtfAvecDelaiCalculator.compute(
                LocalDate.of(2026, 4, 1),
                "AUTRE",
                false,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.dateExpirationDdv()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(r.dateExpirationDelaiRecours()).isEqualTo(r.dateExpirationDdv());
    }
}
