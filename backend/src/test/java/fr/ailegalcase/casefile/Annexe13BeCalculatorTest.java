package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Annexe13BeCalculatorTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Brussels");

    /** Clock fixé pour stabilité des tests temporels. */
    private Clock clockAt(LocalDate today) {
        return Clock.fixed(today.atStartOfDay(ZONE).toInstant(), ZONE);
    }

    // ---- Tests délais ----

    @Test
    void compute_sejourIrregulier_noRecours_disponible() {
        // notif 2026-04-13 (Mon), today 2026-04-15 → annulation expire 2026-05-13, reste 28j → DISPONIBLE
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.motifOqt()).isEqualTo("SEJOUR_IRREGULIER_ART_7");
        assertThat(r.recoursForme()).isFalse();
        assertThat(r.delaiDepartImposeJours()).isEqualTo(30);
        assertThat(r.dateExpirationDelaiDepart()).isEqualTo(LocalDate.of(2026, 5, 13));
        assertThat(r.dateExpirationRecoursAnnulation()).isEqualTo(LocalDate.of(2026, 5, 13));
        assertThat(r.joursRestantsAvantExpirationAnnulation()).isEqualTo(28L);
        assertThat(r.statutRecoursAnnulation()).isEqualTo("DISPONIBLE");
        assertThat(r.dateAudiencePrevisionnelle()).isNull();
        assertThat(r.dateDecisionPrevisionnelle()).isNull();
        assertThat(r.referedDisponibles()).containsExactly("DEMANDE_SUSPENSION_ART_39_2");
        assertThat(r.baseJuridique()).contains("15/12/1980").contains("39/2").contains("AR 08/10/1981");
    }

    @Test
    void compute_extremeUrgence5JoursOuvrables_noHoliday() {
        // notif 2026-04-13 (lundi) : +5 j ouvrables = mar 14 (1), mer 15 (2), jeu 16 (3),
        // ven 17 (4), sam + dim skip, lun 20 (5) → 2026-04-20
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 14)));

        assertThat(r.dateExpirationRecoursExtremeUrgence()).isEqualTo(LocalDate.of(2026, 4, 20));
    }

    @Test
    void compute_extremeUrgence_skipsBelgianPublicHoliday() {
        // notif 2026-04-02 (jeudi) : ven 3 (1), sam/dim skip, lun 6 (Lundi de Pâques férié) skip,
        // mar 7 (2), mer 8 (3), jeu 9 (4), ven 10 (5) → 2026-04-10
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 2),
                30,
                "AUTRE",
                true,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 10)));

        assertThat(r.dateExpirationRecoursExtremeUrgence()).isEqualTo(LocalDate.of(2026, 4, 10));
    }

    @Test
    void compute_delaiDepartZero_emergencyCase() {
        // délai 0 : départ immédiat (risque de fuite) mais recours 30j reste ouvert
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                0,
                "SEJOUR_IRREGULIER_ART_7",
                true,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.delaiDepartImposeJours()).isZero();
        assertThat(r.dateExpirationDelaiDepart()).isEqualTo(LocalDate.of(2026, 4, 13));
        assertThat(r.dateExpirationRecoursAnnulation()).isEqualTo(LocalDate.of(2026, 5, 13));
    }

    @Test
    void compute_urgentStatus_whenLessOrEqual3Days() {
        // notif 2026-04-13, today 2026-05-11 → reste 2 jours → URGENT
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "REFUS_SEJOUR_APRES_DEMANDE",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 5, 11)));

        assertThat(r.joursRestantsAvantExpirationAnnulation()).isEqualTo(2L);
        assertThat(r.statutRecoursAnnulation()).isEqualTo("URGENT");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("URGENT"));
    }

    @Test
    void compute_urgentStatus_exactly3Days() {
        // notif 2026-04-13, today 2026-05-10 → reste 3 jours → URGENT (seuil inclusif)
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "FIN_SEJOUR_REGULIER",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 5, 10)));

        assertThat(r.joursRestantsAvantExpirationAnnulation()).isEqualTo(3L);
        assertThat(r.statutRecoursAnnulation()).isEqualTo("URGENT");
    }

    @Test
    void compute_disponible_4Days() {
        // reste 4 jours → DISPONIBLE
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "AUTRE",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 5, 9)));

        assertThat(r.joursRestantsAvantExpirationAnnulation()).isEqualTo(4L);
        assertThat(r.statutRecoursAnnulation()).isEqualTo("DISPONIBLE");
    }

    @Test
    void compute_expire_whenPastDeadline() {
        // notif 2026-03-01, today 2026-04-15 → expire 2026-03-31, reste -15 → EXPIRE
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 3, 1),
                30,
                "FIN_SEJOUR_REGULIER",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.joursRestantsAvantExpirationAnnulation()).isNegative();
        assertThat(r.statutRecoursAnnulation()).isEqualTo("EXPIRE");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("EXPIRÉ"));
    }

    @Test
    void compute_recoursForme_setsAudienceAndDecisionDates() {
        // recours formé le 2026-04-20 → audience J+30 = 2026-05-20, décision J+90 = 2026-07-19
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                true,
                LocalDate.of(2026, 4, 20),
                "ANNULATION_30J",
                clockAt(LocalDate.of(2026, 4, 25)));

        assertThat(r.statutRecoursAnnulation()).isEqualTo("RECOURS_FORME");
        assertThat(r.joursRestantsAvantExpirationAnnulation()).isZero();
        assertThat(r.typeRecours()).isEqualTo("ANNULATION_30J");
        assertThat(r.dateAudiencePrevisionnelle()).isEqualTo(LocalDate.of(2026, 5, 20));
        assertThat(r.dateDecisionPrevisionnelle()).isEqualTo(LocalDate.of(2026, 7, 19));
        assertThat(r.formule()).contains("J+30").contains("J+90");
    }

    // ---- Tests transfertImminent ----

    @Test
    void compute_transfertImminent_addsExtremeUrgenceRefere() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                0,
                "SEJOUR_IRREGULIER_ART_7",
                true,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 14)));

        assertThat(r.transfertImminent()).isTrue();
        assertThat(r.referedDisponibles())
                .containsExactly("DEMANDE_SUSPENSION_ART_39_2", "RECOURS_EXTREME_URGENCE_39_82");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("extrême urgence"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("39/82"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("72h"));
    }

    @Test
    void compute_noTransfert_onlySuspensionRefere() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.referedDisponibles()).containsExactly("DEMANDE_SUSPENSION_ART_39_2");
    }

    // ---- Tests messages par motif ----

    @Test
    void compute_sejourIrregulierMessage_mentionsArt7() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("art. 7"));
    }

    @Test
    void compute_refusSejourMessage_mentionsDemande() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "REFUS_SEJOUR_APRES_DEMANDE",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("refus"));
    }

    @Test
    void compute_finSejourMessage_mentionsRenouvellement() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "FIN_SEJOUR_REGULIER",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("renouvellement"));
    }

    @Test
    void compute_autreMessage_warnsOperator() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "AUTRE",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Office des étrangers"));
    }

    @Test
    void compute_allMessages_includeCceAndCitations() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "AUTRE",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15)));

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CCE"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("39/2"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("pro deo"));
    }

    @Test
    void compute_allFourMotifsAccepted() {
        for (String motif : java.util.List.of(
                "SEJOUR_IRREGULIER_ART_7", "REFUS_SEJOUR_APRES_DEMANDE",
                "FIN_SEJOUR_REGULIER", "AUTRE")) {
            Annexe13BeResult r = Annexe13BeCalculator.compute(
                    LocalDate.of(2026, 4, 13),
                    30,
                    motif,
                    false,
                    false,
                    null,
                    null,
                    clockAt(LocalDate.of(2026, 4, 15)));
            assertThat(r.motifOqt()).isEqualTo(motif);
            assertThat(r.messages()).isNotEmpty();
        }
    }

    @Test
    void compute_recoursForme_extremeUrgenceType() {
        Annexe13BeResult r = Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                0,
                "SEJOUR_IRREGULIER_ART_7",
                true,
                true,
                LocalDate.of(2026, 4, 16),
                "EXTREME_URGENCE_5JO",
                clockAt(LocalDate.of(2026, 4, 18)));

        assertThat(r.typeRecours()).isEqualTo("EXTREME_URGENCE_5JO");
        assertThat(r.statutRecoursAnnulation()).isEqualTo("RECOURS_FORME");
    }

    // ---- Tests validation ----

    @Test
    void compute_futureNotification_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 5, 1),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void compute_negativeDelaiDepart_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                -1,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("delaiDepartImposeJours");
    }

    @Test
    void compute_nullMotif_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                null,
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifOqt");
    }

    @Test
    void compute_unknownMotif_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "MOTIF_BIDON",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifOqt");
    }

    @Test
    void compute_recoursFormeWithoutDateRecours_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                true,
                null,
                "ANNULATION_30J",
                clockAt(LocalDate.of(2026, 4, 20))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_recoursFormeWithoutTypeRecours_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                true,
                LocalDate.of(2026, 4, 20),
                null,
                clockAt(LocalDate.of(2026, 4, 25))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRecours");
    }

    @Test
    void compute_unknownTypeRecours_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                true,
                LocalDate.of(2026, 4, 20),
                "TYPE_BIDON",
                clockAt(LocalDate.of(2026, 4, 25))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeRecours");
    }

    @Test
    void compute_dateRecoursBeforeNotification_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                LocalDate.of(2026, 4, 13),
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                true,
                LocalDate.of(2026, 4, 10),
                "ANNULATION_30J",
                clockAt(LocalDate.of(2026, 4, 20))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_nullDateNotification_throws() {
        assertThatThrownBy(() -> Annexe13BeCalculator.compute(
                null,
                30,
                "SEJOUR_IRREGULIER_ART_7",
                false,
                false,
                null,
                null,
                clockAt(LocalDate.of(2026, 4, 15))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationAnnexe13");
    }
}
