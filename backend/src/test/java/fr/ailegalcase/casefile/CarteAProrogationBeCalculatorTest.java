package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-221-01 — tests unitaires du calculateur de prorogation de la carte A BE
 * (art. 13 Loi 15/12/1980 + art. 33 AR 08/10/1981). Couvre les 5 verdicts,
 * le calcul de la fenêtre 30-45 j et joursAvantExpiration.
 */
class CarteAProrogationBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 3);

    @Test
    void prorogeable_dansLaFenetre() {
        // Expiration dans 40 jours → dans la fenêtre 30-45, conditions OK.
        LocalDate exp = TODAY.plusDays(40);
        CarteAProrogationBeResult r = CarteAProrogationBeCalculator.compute(
                exp, true, true, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteAProrogationBeVerdict.PROROGEABLE);
        assertThat(r.joursAvantExpiration()).isEqualTo(40);
        assertThat(r.dateOuvertureFenetre()).isEqualTo(exp.minusDays(45));
        assertThat(r.dateLimiteRecommandee()).isEqualTo(exp.minusDays(30));
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("art. 13"));
    }

    @Test
    void aDeposerUrgent_fenetreDepasseeMaisNonExpiree() {
        // Expiration dans 20 jours → fenêtre dépassée (< 30) mais carte valide, pas déposée.
        LocalDate exp = TODAY.plusDays(20);
        CarteAProrogationBeResult r = CarteAProrogationBeCalculator.compute(
                exp, true, true, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteAProrogationBeVerdict.A_DEPOSER_URGENT);
        assertThat(r.joursAvantExpiration()).isEqualTo(20);
    }

    @Test
    void conditionsNonReunies_motifNePersistePlus() {
        LocalDate exp = TODAY.plusDays(40);
        CarteAProrogationBeResult r = CarteAProrogationBeCalculator.compute(
                exp, false, true, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteAProrogationBeVerdict.CONDITIONS_NON_REUNIES);
    }

    @Test
    void conditionsNonReunies_conditionsInitialesNonReunies() {
        LocalDate exp = TODAY.plusDays(40);
        CarteAProrogationBeResult r = CarteAProrogationBeCalculator.compute(
                exp, true, false, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteAProrogationBeVerdict.CONDITIONS_NON_REUNIES);
    }

    @Test
    void expiree_carteDepasseeNonDeposee() {
        LocalDate exp = TODAY.minusDays(5);
        CarteAProrogationBeResult r = CarteAProrogationBeCalculator.compute(
                exp, true, true, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteAProrogationBeVerdict.EXPIREE);
        assertThat(r.joursAvantExpiration()).isEqualTo(-5);
    }

    @Test
    void demandeDeposee_prioriteSurAutresVerdicts() {
        LocalDate exp = TODAY.minusDays(5); // serait EXPIREE mais déposée
        CarteAProrogationBeResult r = CarteAProrogationBeCalculator.compute(
                exp, true, true, true, TODAY.minusDays(10), TODAY);

        assertThat(r.verdict()).isEqualTo(CarteAProrogationBeVerdict.DEMANDE_DEPOSEE);
        assertThat(r.dateDemande()).isEqualTo(TODAY.minusDays(10));
    }

    @Test
    void fenetre_borneHaute_45jours_estProrogeable() {
        LocalDate exp = TODAY.plusDays(45);
        CarteAProrogationBeResult r = CarteAProrogationBeCalculator.compute(
                exp, true, true, false, null, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteAProrogationBeVerdict.PROROGEABLE);
    }

    @Test
    void validation_dateExpirationNull_jette() {
        assertThatThrownBy(() -> CarteAProrogationBeCalculator.compute(
                null, true, true, false, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_demandeDeposeeSansDate_jette() {
        assertThatThrownBy(() -> CarteAProrogationBeCalculator.compute(
                TODAY.plusDays(10), true, true, true, null, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateDemande");
    }
}
