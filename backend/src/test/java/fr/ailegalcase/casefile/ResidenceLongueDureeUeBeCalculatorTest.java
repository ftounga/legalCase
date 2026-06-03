package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-221-03 — tests unitaires du calculateur résident longue durée UE BE
 * (art. 15bis Loi 15/12/1980 — directive 2003/109/CE). Couvre les 5 verdicts,
 * le seuil 60 mois, l'énumération des conditions manquantes et la continuité.
 */
class ResidenceLongueDureeUeBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 3);

    @Test
    void eligible_cinqAnsToutesConditionsReunies() {
        // 5 ans + 2 mois de séjour légal ininterrompu, toutes conditions OK, pas d'absences hors UE.
        LocalDate debut = TODAY.minusYears(5).minusMonths(2);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, true, true, true, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(ResidenceLongueDureeUeBeVerdict.ELIGIBLE);
        assertThat(r.dureeSejourMois()).isEqualTo(62);
        assertThat(r.moisRestants()).isZero();
        assertThat(r.conditionsManquantes()).isEmpty();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("art. 15bis"));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("2003/109"));
    }

    @Test
    void eligible_borneExacte_60mois() {
        LocalDate debut = TODAY.minusMonths(60);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, true, true, true, true, false, TODAY);

        assertThat(r.dureeSejourMois()).isEqualTo(60);
        assertThat(r.verdict()).isEqualTo(ResidenceLongueDureeUeBeVerdict.ELIGIBLE);
    }

    @Test
    void dureeInsuffisante_moinsDe60mois() {
        // 3 ans = 36 mois → 24 mois restants (prioritaire sur les conditions matérielles).
        LocalDate debut = TODAY.minusMonths(36);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, true, false, false, false, false, TODAY);

        assertThat(r.verdict()).isEqualTo(ResidenceLongueDureeUeBeVerdict.DUREE_INSUFFISANTE);
        assertThat(r.dureeSejourMois()).isEqualTo(36);
        assertThat(r.moisRestants()).isEqualTo(24);
    }

    @Test
    void continuiteRompue_sejourNonIninterrompu() {
        LocalDate debut = TODAY.minusYears(6);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, false, true, true, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(ResidenceLongueDureeUeBeVerdict.CONTINUITE_ROMPUE);
    }

    @Test
    void continuiteRompue_absencesHorsUeExcessives() {
        LocalDate debut = TODAY.minusYears(6);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, true, true, true, true, true, TODAY);

        assertThat(r.verdict()).isEqualTo(ResidenceLongueDureeUeBeVerdict.CONTINUITE_ROMPUE);
    }

    @Test
    void conditionsMaterielles_nonReunies_listeConditionsManquantes() {
        // Durée + continuité OK, pas d'absences excessives, mais ressources + assurance manquent.
        LocalDate debut = TODAY.minusYears(6);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, true, false, false, true, false, TODAY);

        assertThat(r.verdict())
                .isEqualTo(ResidenceLongueDureeUeBeVerdict.CONDITIONS_MATERIELLES_NON_REUNIES);
        assertThat(r.conditionsManquantes()).hasSize(2);
        assertThat(r.conditionsManquantes())
                .anyMatch(c -> c.toLowerCase().contains("ressources"));
        assertThat(r.conditionsManquantes())
                .anyMatch(c -> c.toLowerCase().contains("assurance"));
    }

    @Test
    void conditionsMaterielles_integrationSeule_manquante() {
        LocalDate debut = TODAY.minusYears(6);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, true, true, true, false, false, TODAY);

        assertThat(r.verdict())
                .isEqualTo(ResidenceLongueDureeUeBeVerdict.CONDITIONS_MATERIELLES_NON_REUNIES);
        assertThat(r.conditionsManquantes()).hasSize(1);
        assertThat(r.conditionsManquantes().get(0).toLowerCase()).contains("intégration");
    }

    @Test
    void continuite_prioritaire_surConditionsMaterielles() {
        // Continuité rompue ET conditions matérielles manquantes → CONTINUITE_ROMPUE prime.
        LocalDate debut = TODAY.minusYears(6);
        ResidenceLongueDureeUeBeResult r = ResidenceLongueDureeUeBeCalculator.compute(
                debut, false, false, false, false, false, TODAY);

        assertThat(r.verdict()).isEqualTo(ResidenceLongueDureeUeBeVerdict.CONTINUITE_ROMPUE);
    }

    @Test
    void validation_dateDebutNull_jette() {
        assertThatThrownBy(() -> ResidenceLongueDureeUeBeCalculator.compute(
                null, true, true, true, true, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_dateDebutFuture_jette() {
        assertThatThrownBy(() -> ResidenceLongueDureeUeBeCalculator.compute(
                TODAY.plusDays(1), true, true, true, true, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }
}
