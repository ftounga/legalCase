package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-13 — UT du calculator du recours en annulation CCE (BE).
 *
 * <p>Source : Loi 15/12/1980 art. 39/2 §2 et 39/82 §4 al. 1 — 30 jours CALENDAIRES.
 * Outil BELGIQUE UNIQUEMENT (Conseil du Contentieux des Étrangers, droit des étrangers).
 */
class CceAnnulationBeCalculatorTest {

    private static final CceAnnulationBeTypeDecisionEnum OQT = CceAnnulationBeTypeDecisionEnum.OQT_ANNEXE13;

    @Test
    void compute_dateLimite_estNotificationPlus30JoursCalendaires() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, false, null, notif);

        assertThat(r.dateLimiteRecours()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(r.joursRestants()).isEqualTo(30);
    }

    @Test
    void compute_joursRestantsSuperieur10_returnsDisponible() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 10); // 21 jours restants
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, false, null, today);

        assertThat(r.joursRestants()).isEqualTo(21);
        assertThat(r.statut()).isEqualTo(CceAnnulationBeStatut.DISPONIBLE);
        assertThat(r.recommandation()).doesNotContain("F-IM-32");
    }

    @Test
    void compute_borneHaute10jours_returnsUrgent() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 21); // 10 jours restants
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, false, null, today);

        assertThat(r.joursRestants()).isEqualTo(10);
        assertThat(r.statut()).isEqualTo(CceAnnulationBeStatut.URGENT);
        assertThat(r.recommandation()).contains("F-IM-32");
    }

    @Test
    void compute_borneBasse1jour_returnsUrgent() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 30); // 1 jour restant
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, false, null, today);

        assertThat(r.joursRestants()).isEqualTo(1);
        assertThat(r.statut()).isEqualTo(CceAnnulationBeStatut.URGENT);
        assertThat(r.recommandation()).contains("F-IM-32");
    }

    @Test
    void compute_jourLimite0jours_returnsExpire() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 31); // 0 jour restant → EXPIRE (≤ 0)
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, false, null, today);

        assertThat(r.joursRestants()).isZero();
        assertThat(r.statut()).isEqualTo(CceAnnulationBeStatut.EXPIRE);
        assertThat(r.recommandation()).contains("F-IM-32");
    }

    @Test
    void compute_apresDateLimite_returnsExpire_joursNegatif() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 2, 10);
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, false, null, today);

        assertThat(r.joursRestants()).isNegative();
        assertThat(r.statut()).isEqualTo(CceAnnulationBeStatut.EXPIRE);
        assertThat(r.recommandation()).contains("F-IM-32");
    }

    @Test
    void compute_recoursForme_returnsRecoursForme_prioritaireSurExpire() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 3, 1); // délai expiré...
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, true, LocalDate.of(2026, 1, 15), today);

        // ...mais recoursForme=true est prioritaire
        assertThat(r.statut()).isEqualTo(CceAnnulationBeStatut.RECOURS_FORME);
        assertThat(r.recoursForme()).isTrue();
        assertThat(r.dateRecours()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void compute_delaisMemoire_contientReference39_63() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        CceAnnulationBeResult r = CceAnnulationBeCalculator.compute(
                notif, OQT, false, null, notif);

        assertThat(r.delaisMemoire())
                .contains("mémoire en réplique 8 jours")
                .contains("39/63");
        assertThat(r.baseJuridique()).contains("39/82");
    }

    @Test
    void compute_dateNotificationFuture_throwsIllegalArgument() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate notifFuture = LocalDate.of(2026, 1, 2);
        assertThatThrownBy(() -> CceAnnulationBeCalculator.compute(
                notifFuture, OQT, false, null, today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationDecision");
    }

    @Test
    void compute_recoursFormeSansDateRecours_throwsIllegalArgument() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> CceAnnulationBeCalculator.compute(
                notif, OQT, true, null, notif))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_dateRecoursAvantNotification_throwsIllegalArgument() {
        LocalDate notif = LocalDate.of(2026, 1, 10);
        assertThatThrownBy(() -> CceAnnulationBeCalculator.compute(
                notif, OQT, true, LocalDate.of(2026, 1, 5), notif))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }

    @Test
    void compute_typeDecisionNull_throwsIllegalArgument() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> CceAnnulationBeCalculator.compute(
                notif, null, false, null, notif))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("typeDecision");
    }
}
