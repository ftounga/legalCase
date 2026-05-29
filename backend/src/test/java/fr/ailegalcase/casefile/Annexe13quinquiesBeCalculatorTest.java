package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-215-17 — UT du calculator de l'Annexe 13quinquies (OQT + interdiction d'entrée,
 * art. 74/11 Loi 15/12/1980).
 *
 * <p>Durées 3/5/8 ans (art. 74/11 §2) ; levée anticipée à 2/3 du délai (art. 74/12) ;
 * recours en annulation CCE 30 jours CALENDAIRES (art. 39/82 §4 al. 1).
 * Outil BELGIQUE UNIQUEMENT (droit des étrangers).
 */
class Annexe13quinquiesBeCalculatorTest {

    @Test
    void compute_sejourIrregulierSansPrecedentSejour_duree3ans() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, notif);

        assertThat(r.dureeInterdiction()).isEqualTo(3);
        assertThat(r.dateFinInterdiction()).isEqualTo(LocalDate.of(2029, 1, 1));
    }

    @Test
    void compute_sejourIrregulierAvecPrecedentSejour_duree5ans() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, true, false, null, notif);

        assertThat(r.dureeInterdiction()).isEqualTo(5);
        assertThat(r.dateFinInterdiction()).isEqualTo(LocalDate.of(2031, 1, 1));
    }

    @Test
    void compute_menaceOrdrePublic_duree5ans() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.MENACE_ORDRE_PUBLIC, false, false, null, notif);

        assertThat(r.dureeInterdiction()).isEqualTo(5);
    }

    @Test
    void compute_securiteNationale_duree5ans() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.RAISONS_SECURITE_NATIONALE, false, false, null, notif);

        assertThat(r.dureeInterdiction()).isEqualTo(5);
    }

    @Test
    void compute_atteinteInteretUe_duree8ans() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.ATTEINTE_INTERET_UE, false, false, null, notif);

        assertThat(r.dureeInterdiction()).isEqualTo(8);
        assertThat(r.dateFinInterdiction()).isEqualTo(LocalDate.of(2034, 1, 1));
    }

    @Test
    void compute_decisionJudiciaire_duree8ans() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.DECISION_JUDICIAIRE, false, false, null, notif);

        assertThat(r.dureeInterdiction()).isEqualTo(8);
    }

    @Test
    void compute_levePrecoce_estDeuxTiersDuDelai_3ans() {
        // 3 ans → 2/3 × 36 mois = 24 mois
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, notif);

        assertThat(r.datePossibleLevePrecoce()).isEqualTo(LocalDate.of(2028, 1, 1));
    }

    @Test
    void compute_levePrecoce_estDeuxTiersDuDelai_8ans() {
        // 8 ans → 2/3 × 96 mois = 64 mois
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.DECISION_JUDICIAIRE, false, false, null, notif);

        assertThat(r.datePossibleLevePrecoce()).isEqualTo(LocalDate.of(2031, 5, 1));
    }

    @Test
    void compute_dateLimiteRecours_estNotificationPlus30JoursCalendaires() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, notif);

        assertThat(r.dateLimiteRecoursAnnulation()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(r.joursRestantsRecours()).isEqualTo(30);
    }

    @Test
    void compute_joursRestantsSuperieur10_returnsDisponible() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 10); // 21 jours restants
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, today);

        assertThat(r.joursRestantsRecours()).isEqualTo(21);
        assertThat(r.statutRecours()).isEqualTo(Annexe13quinquiesBeStatut.DISPONIBLE);
    }

    @Test
    void compute_borneHaute10jours_returnsUrgent() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 1, 21); // 10 jours restants
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, today);

        assertThat(r.joursRestantsRecours()).isEqualTo(10);
        assertThat(r.statutRecours()).isEqualTo(Annexe13quinquiesBeStatut.URGENT);
    }

    @Test
    void compute_apresDateLimite_returnsExpire() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 2, 10);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, today);

        assertThat(r.joursRestantsRecours()).isNegative();
        assertThat(r.statutRecours()).isEqualTo(Annexe13quinquiesBeStatut.EXPIRE);
    }

    @Test
    void compute_recoursForme_returnsForme_prioritaireSurExpire() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        LocalDate today = LocalDate.of(2026, 3, 1); // délai expiré...
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, true,
                LocalDate.of(2026, 1, 15), today);

        // ...mais recoursForme=true est prioritaire
        assertThat(r.statutRecours()).isEqualTo(Annexe13quinquiesBeStatut.FORME);
        assertThat(r.recoursForme()).isTrue();
        assertThat(r.dateRecours()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void compute_conditionsLevee_nonVide_referenceArt7412() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, notif);

        assertThat(r.conditionsLevee()).isNotEmpty();
        assertThat(r.baseJuridique()).contains("74/11").contains("74/12").contains("39/82");
    }

    @Test
    void compute_dateNotificationFutureToleree1Jour_ok() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate notifDemain = LocalDate.of(2026, 1, 2); // tolérance fuseau (+1j)
        Annexe13quinquiesBeResult r = Annexe13quinquiesBeCalculator.compute(
                notifDemain, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, today);

        assertThat(r.dureeInterdiction()).isEqualTo(3);
    }

    @Test
    void compute_dateNotificationFutureSuperieure1Jour_throwsIllegalArgument() {
        LocalDate today = LocalDate.of(2026, 1, 1);
        LocalDate notifFuture = LocalDate.of(2026, 1, 5);
        assertThatThrownBy(() -> Annexe13quinquiesBeCalculator.compute(
                notifFuture, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, false, null, today))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationAnnexe");
    }

    @Test
    void compute_motifNull_throwsIllegalArgument() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> Annexe13quinquiesBeCalculator.compute(
                notif, null, false, false, null, notif))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("motifInterdictionEntree");
    }

    @Test
    void compute_precedentSejourNull_throwsIllegalArgument() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, null, false, null, notif))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("precedentSejour");
    }

    @Test
    void compute_recoursFormeSansDateRecours_throwsIllegalArgument() {
        LocalDate notif = LocalDate.of(2026, 1, 1);
        assertThatThrownBy(() -> Annexe13quinquiesBeCalculator.compute(
                notif, Annexe13quinquiesBeMotifEnum.SEJOUR_IRREGULIER, false, true, null, notif))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateRecours");
    }
}
