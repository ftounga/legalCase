package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-221-04 — tests unitaires du calculateur détention en centre fermé + requête de
 * mise en liberté BE (Loi 15/12/1980 art. 7/27/29/74/5 + 71 et s. ; AR 02/08/2002).
 * Couvre les 5 verdicts, la durée de détention, la fenêtre indicative de 5 jours,
 * la requête tardive, la prolongation et les validations conditionnelles.
 */
class DetentionCentreFermeBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 3);

    @Test
    void detentionEnCours_sansNotification_default() {
        LocalDate debut = TODAY.minusDays(10);
        DetentionCentreFermeBeResult r = DetentionCentreFermeBeCalculator.compute(
                debut, DetentionBaseLegale.ART_7, false, null, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(DetentionCentreFermeBeVerdict.DETENTION_EN_COURS);
        assertThat(r.dureeDetentionJours()).isEqualTo(10);
        assertThat(r.dateLimiteRequete()).isNull();
        assertThat(r.joursRestantsRequete()).isNull();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("art. 71"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("AR du 02/08/2002"));
    }

    @Test
    void requeteOuverte_dansFenetre5j() {
        LocalDate debut = TODAY.minusDays(4);
        LocalDate notif = TODAY.minusDays(2);
        DetentionCentreFermeBeResult r = DetentionCentreFermeBeCalculator.compute(
                debut, DetentionBaseLegale.ART_74_5, false, null, false, notif, TODAY);

        assertThat(r.verdict()).isEqualTo(DetentionCentreFermeBeVerdict.REQUETE_OUVERTE);
        assertThat(r.dateLimiteRequete()).isEqualTo(notif.plusDays(5));
        assertThat(r.joursRestantsRequete()).isEqualTo(3);
    }

    @Test
    void requeteOuverte_borneExacte_jour5() {
        LocalDate debut = TODAY.minusDays(6);
        LocalDate notif = TODAY.minusDays(5);
        DetentionCentreFermeBeResult r = DetentionCentreFermeBeCalculator.compute(
                debut, DetentionBaseLegale.ART_27, false, null, false, notif, TODAY);

        assertThat(r.verdict()).isEqualTo(DetentionCentreFermeBeVerdict.REQUETE_OUVERTE);
        assertThat(r.joursRestantsRequete()).isZero();
    }

    @Test
    void requeteTardive_au_dela_de_5j() {
        LocalDate debut = TODAY.minusDays(20);
        LocalDate notif = TODAY.minusDays(10);
        DetentionCentreFermeBeResult r = DetentionCentreFermeBeCalculator.compute(
                debut, DetentionBaseLegale.ART_29, false, null, false, notif, TODAY);

        assertThat(r.verdict()).isEqualTo(DetentionCentreFermeBeVerdict.REQUETE_TARDIVE);
        assertThat(r.joursRestantsRequete()).isZero();
    }

    @Test
    void requeteDeposee_prioritaire() {
        LocalDate debut = TODAY.minusDays(3);
        LocalDate notif = TODAY.minusDays(1);
        DetentionCentreFermeBeResult r = DetentionCentreFermeBeCalculator.compute(
                debut, DetentionBaseLegale.ART_7, false, null, true, notif, TODAY);

        assertThat(r.verdict()).isEqualTo(DetentionCentreFermeBeVerdict.REQUETE_DEPOSEE);
        assertThat(r.requeteMiseEnLiberteDeposee()).isTrue();
    }

    @Test
    void prolongationAContester_recalculeFenetreDepuisProlongation() {
        LocalDate debut = TODAY.minusDays(30);
        LocalDate notif = TODAY.minusDays(28);
        LocalDate prolongation = TODAY.minusDays(2);
        DetentionCentreFermeBeResult r = DetentionCentreFermeBeCalculator.compute(
                debut, DetentionBaseLegale.ART_7, true, prolongation, false, notif, TODAY);

        assertThat(r.verdict()).isEqualTo(DetentionCentreFermeBeVerdict.PROLONGATION_A_CONTESTER);
        assertThat(r.dateProlongation()).isEqualTo(prolongation);
        assertThat(r.dateLimiteRequete()).isEqualTo(prolongation.plusDays(5));
        assertThat(r.joursRestantsRequete()).isEqualTo(3);
    }

    @Test
    void dureeDetention_estCalculee() {
        LocalDate debut = TODAY.minusDays(45);
        DetentionCentreFermeBeResult r = DetentionCentreFermeBeCalculator.compute(
                debut, DetentionBaseLegale.AUTRE, false, null, false, null, TODAY);
        assertThat(r.dureeDetentionJours()).isEqualTo(45);
    }

    @Test
    void validation_dateDebutNull_jette() {
        assertThatThrownBy(() -> DetentionCentreFermeBeCalculator.compute(
                null, DetentionBaseLegale.ART_7, false, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_dateDebutFuture_jette() {
        assertThatThrownBy(() -> DetentionCentreFermeBeCalculator.compute(
                TODAY.plusDays(1), DetentionBaseLegale.ART_7, false, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void validation_prolongationSansDate_jette() {
        assertThatThrownBy(() -> DetentionCentreFermeBeCalculator.compute(
                TODAY.minusDays(5), DetentionBaseLegale.ART_7, true, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateProlongation");
    }

    @Test
    void validation_requeteDeposeeSansNotification_jette() {
        assertThatThrownBy(() -> DetentionCentreFermeBeCalculator.compute(
                TODAY.minusDays(5), DetentionBaseLegale.ART_7, false, null, true, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNotificationDecisionDetention");
    }

    @Test
    void validation_baseLegaleNull_jette() {
        assertThatThrownBy(() -> DetentionCentreFermeBeCalculator.compute(
                TODAY.minusDays(5), null, false, null, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("baseLegaleDetention");
    }
}
