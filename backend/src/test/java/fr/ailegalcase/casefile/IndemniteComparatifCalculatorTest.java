package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndemniteComparatifCalculatorTest {

    @Test
    void france_10ans_35ans_returnsValidResult() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "FRANCE", 10, 35, new BigDecimal("3000"));

        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.ancienneteAnnees()).isEqualTo(10);
        assertThat(r.baremePlancherMois()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(r.baremePlafondMois()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(r.fourchetteBasseMois()).isPositive();
        assertThat(r.fourchetteMedMois()).isGreaterThanOrEqualTo(r.fourchetteBasseMois());
        assertThat(r.fourhetteHauteMois()).isGreaterThanOrEqualTo(r.fourchetteMedMois());
        assertThat(r.fourchetteBasseMontant()).isPositive();
        assertThat(r.baremeSource()).contains("Macron");
    }

    @Test
    void france_senior_higherCommentaire() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "FRANCE", 15, 55, new BigDecimal("4000"));
        assertThat(r.commentaire()).contains("50 ans");
    }

    @Test
    void belgique_5ans_returnsValidResult() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "BELGIQUE", 5, 40, new BigDecimal("2500"));

        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.fourchetteBasseMois()).isPositive();
        assertThat(r.fourhetteHauteMois()).isGreaterThanOrEqualTo(r.fourchetteBasseMois());
        assertThat(r.baremeSource()).contains("CCT");
    }

    @Test
    void belgique_highAnciennete_commentaire() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "BELGIQUE", 12, 45, new BigDecimal("3000"));
        assertThat(r.commentaire()).contains("Ancienneté significative");
    }

    @Test
    void montantsCalculated_correctly() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "FRANCE", 5, 30, new BigDecimal("2000"));
        // montant = salaire × fourchetteMois
        assertThat(r.fourchetteBasseMontant())
                .isEqualByComparingTo(r.salaireMensuel().multiply(r.fourchetteBasseMois()).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void invalidCountry_throws() {
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "ALLEMAGNE", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
