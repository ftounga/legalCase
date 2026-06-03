package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SF-221-02 — tests unitaires du calculateur carte B séjour illimité BE
 * (art. 14 Loi 15/12/1980). Couvre les 5 verdicts, le seuil 60 mois et
 * le calcul de dureeSejourMois / moisRestants.
 */
class CarteBSejourIllimiteBeCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 3);

    @Test
    void eligible_cinqAnsConditionsReunies() {
        // 5 ans + 2 mois de séjour régulier ininterrompu, motif stable, pas d'ordre public.
        LocalDate debut = TODAY.minusYears(5).minusMonths(2);
        CarteBSejourIllimiteBeResult r = CarteBSejourIllimiteBeCalculator.compute(
                debut, true, false, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteBSejourIllimiteBeVerdict.ELIGIBLE);
        assertThat(r.dureeSejourMois()).isEqualTo(62);
        assertThat(r.moisRestants()).isZero();
        assertThat(r.basesJuridiques()).anyMatch(b -> b.contains("art. 14"));
    }

    @Test
    void eligible_borneExacte_60mois() {
        LocalDate debut = TODAY.minusMonths(60);
        CarteBSejourIllimiteBeResult r = CarteBSejourIllimiteBeCalculator.compute(
                debut, true, false, true, false, TODAY);

        assertThat(r.dureeSejourMois()).isEqualTo(60);
        assertThat(r.verdict()).isEqualTo(CarteBSejourIllimiteBeVerdict.ELIGIBLE);
    }

    @Test
    void dureeInsuffisante_moinsDe60mois() {
        // 3 ans = 36 mois → 24 mois restants.
        LocalDate debut = TODAY.minusMonths(36);
        CarteBSejourIllimiteBeResult r = CarteBSejourIllimiteBeCalculator.compute(
                debut, true, false, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteBSejourIllimiteBeVerdict.DUREE_INSUFFISANTE);
        assertThat(r.dureeSejourMois()).isEqualTo(36);
        assertThat(r.moisRestants()).isEqualTo(24);
    }

    @Test
    void continuiteRompue_sejourNonIninterrompu() {
        LocalDate debut = TODAY.minusYears(6);
        CarteBSejourIllimiteBeResult r = CarteBSejourIllimiteBeCalculator.compute(
                debut, false, false, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteBSejourIllimiteBeVerdict.CONTINUITE_ROMPUE);
    }

    @Test
    void continuiteRompue_absencesSuperieuresLimites() {
        LocalDate debut = TODAY.minusYears(6);
        CarteBSejourIllimiteBeResult r = CarteBSejourIllimiteBeCalculator.compute(
                debut, true, true, true, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteBSejourIllimiteBeVerdict.CONTINUITE_ROMPUE);
    }

    @Test
    void risqueOrdrePublic_prioritaire() {
        // Durée et continuité OK mais risque d'ordre public → prioritaire.
        LocalDate debut = TODAY.minusYears(6);
        CarteBSejourIllimiteBeResult r = CarteBSejourIllimiteBeCalculator.compute(
                debut, true, false, true, true, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteBSejourIllimiteBeVerdict.RISQUE_ORDRE_PUBLIC);
    }

    @Test
    void aExaminer_motifInstable() {
        // Durée + continuité OK, pas d'ordre public, mais motif non stable → A_EXAMINER (default).
        LocalDate debut = TODAY.minusYears(6);
        CarteBSejourIllimiteBeResult r = CarteBSejourIllimiteBeCalculator.compute(
                debut, true, false, false, false, TODAY);

        assertThat(r.verdict()).isEqualTo(CarteBSejourIllimiteBeVerdict.A_EXAMINER);
    }

    @Test
    void validation_dateDebutNull_jette() {
        assertThatThrownBy(() -> CarteBSejourIllimiteBeCalculator.compute(
                null, true, false, true, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validation_dateDebutFuture_jette() {
        assertThatThrownBy(() -> CarteBSejourIllimiteBeCalculator.compute(
                TODAY.plusDays(1), true, false, true, false, TODAY))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }
}
