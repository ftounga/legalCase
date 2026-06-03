package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-221-05 — tests unitaires du calculateur recours CCE en SUSPENSION ORDINAIRE BE
 * (référé administratif art. 39/82 Loi 15/12/1980 ; loi 15/09/2006). Couvre les
 * 5 verdicts, joursDepuisNotification, le renvoi cross-outil extrême urgence (F-IM-32),
 * le hors délai d'annulation et les validations.
 */
class CceSuspensionBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 3);

    @Test
    void conditionsReunies_urgenceEtPrejudice_dansDelai() {
        LocalDate notif = TODAY.minusDays(10);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, false, true, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.CONDITIONS_REUNIES);
        assertThat(r.joursDepuisNotification()).isEqualTo(10);
        assertThat(r.renvoiOutil()).isNull();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("art. 39/82"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("15/09/2006"));
    }

    @Test
    void conditionsReunies_annulationDejaIntroduite_horsDelai() {
        LocalDate notif = TODAY.minusDays(45);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, true, true, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.CONDITIONS_REUNIES);
        assertThat(r.recoursAnnulationIntroduit()).isTrue();
    }

    @Test
    void reorienterExtremeUrgence_prioritaire_renvoiVersF32() {
        LocalDate notif = TODAY.minusDays(2);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, false, true, true, true, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.REORIENTER_EXTREME_URGENCE);
        assertThat(r.renvoiOutil()).isEqualTo("F-IM-32-cce-extreme-urgence-5j-be");
    }

    @Test
    void urgenceNonDemontree() {
        LocalDate notif = TODAY.minusDays(5);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, true, false, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.URGENCE_NON_DEMONTREE);
        assertThat(r.renvoiOutil()).isNull();
    }

    @Test
    void prejudiceNonDemontre() {
        LocalDate notif = TODAY.minusDays(5);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, true, true, false, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.PREJUDICE_NON_DEMONTRE);
    }

    @Test
    void horsDelaiAnnulation_pasDeRecoursIntroduit_au_dela_30j() {
        LocalDate notif = TODAY.minusDays(40);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, false, true, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.HORS_DELAI_ANNULATION);
        assertThat(r.joursDepuisNotification()).isEqualTo(40);
    }

    @Test
    void dansDelaiBorneExacte_jour30_conditionsReunies() {
        LocalDate notif = TODAY.minusDays(30);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, false, true, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.CONDITIONS_REUNIES);
    }

    @Test
    void extremeUrgence_primeSurUrgenceNonDemontree() {
        // Même sans urgence ordinaire invoquée, l'éloignement imminent réoriente.
        LocalDate notif = TODAY.minusDays(2);
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, false, false, false, true, TODAY);

        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.REORIENTER_EXTREME_URGENCE);
        assertThat(r.renvoiOutil()).isEqualTo("F-IM-32-cce-extreme-urgence-5j-be");
    }

    @Test
    void validation_dateNotificationNull_jette() {
        assertThatThrownBy(() -> CceSuspensionBeCalculator.compute(
                null, false, true, true, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_dateNotificationFuture_jette() {
        assertThatThrownBy(() -> CceSuspensionBeCalculator.compute(
                TODAY.plusDays(1), false, true, true, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void nullBooleans_traitesCommeFalse() {
        LocalDate notif = TODAY.minusDays(5);
        // urgence null -> URGENCE_NON_DEMONTREE (faute d'urgence démontrée).
        CceSuspensionBeResult r = CceSuspensionBeCalculator.compute(
                notif, null, null, null, null, TODAY);
        assertThat(r.verdict()).isEqualTo(CceSuspensionBeVerdict.URGENCE_NON_DEMONTREE);
        assertThat(r.recoursAnnulationIntroduit()).isFalse();
        assertThat(r.mesureEloignementImminente()).isFalse();
    }
}
