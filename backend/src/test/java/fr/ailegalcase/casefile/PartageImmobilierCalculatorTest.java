package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PartageImmobilierCalculatorTest {

    @Test
    void france_5050_soulteCorrect() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "FRANCE", bd("300000"), bd("100000"), bd("0.50"), true);

        assertThat(r.valeurNette()).isEqualByComparingTo(bd("200000"));
        assertThat(r.soulte()).isEqualByComparingTo(bd("100000.00")); // 50% de 200000
        assertThat(r.tauxDroitPartage()).isEqualByComparingTo(bd("1.1"));
        assertThat(r.droitPartage()).isEqualByComparingTo(bd("2200.00")); // 1.1% de 200000
    }

    @Test
    void france_7030_soulteCorrect() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "FRANCE", bd("400000"), bd("150000"), bd("0.70"), false);

        // Valeur nette = 250000, part attributaire = 175000, part cédant = 75000
        assertThat(r.valeurNette()).isEqualByComparingTo(bd("250000"));
        assertThat(r.partAttributaire()).isEqualByComparingTo(bd("175000.00"));
        assertThat(r.soulte()).isEqualByComparingTo(bd("75000.00"));
    }

    @Test
    void belgique_divorce_tauxReduit1pct() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "BELGIQUE", bd("350000"), bd("80000"), bd("0.50"), true);

        assertThat(r.tauxDroitPartage()).isEqualByComparingTo(bd("1"));
        assertThat(r.droitPartage()).isEqualByComparingTo(bd("2700.00")); // 1% de 270000
        assertThat(r.baseJuridique()).contains("1 %");
    }

    @Test
    void belgique_nonDivorce_taux25pct() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "BELGIQUE", bd("350000"), bd("80000"), bd("0.50"), false);

        assertThat(r.tauxDroitPartage()).isEqualByComparingTo(bd("2.5"));
        assertThat(r.droitPartage()).isEqualByComparingTo(bd("6750.00")); // 2.5% de 270000
    }

    @Test
    void coutTotal_inclusSoulteDroitFrais() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "FRANCE", bd("200000"), bd("0"), bd("0.50"), true);

        // soulte=100000, droit=2200 (1.1% de 200000), frais=3000 (1.5% de 200000)
        assertThat(r.coutTotal()).isEqualByComparingTo(
                r.soulte().add(r.droitPartage()).add(r.fraisNotaireEstimes()));
    }

    @Test
    void sansPret_valeurNette_egaleVenale() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "FRANCE", bd("500000"), bd("0"), bd("0.50"), true);

        assertThat(r.valeurNette()).isEqualByComparingTo(bd("500000"));
    }

    @Test
    void quotePartComplete_soulteZero() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "FRANCE", bd("300000"), bd("100000"), bd("1.00"), true);

        // L'attributaire prend tout → soulte = part du cédant = 0
        assertThat(r.soulte()).isEqualByComparingTo(bd("0.00"));
    }

    @Test
    void fraisNotaire_france15pct() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "FRANCE", bd("200000"), bd("0"), bd("0.50"), true);
        assertThat(r.fraisNotaireEstimes()).isEqualByComparingTo(bd("3000.00")); // 1.5% de 200000
    }

    @Test
    void fraisNotaire_belgique1pct() {
        PartageImmobilierResult r = PartageImmobilierCalculator.calculate(
                "BELGIQUE", bd("200000"), bd("0"), bd("0.50"), true);
        assertThat(r.fraisNotaireEstimes()).isEqualByComparingTo(bd("2000.00")); // 1% de 200000
    }

    @Test
    void invalidCountry_throws() {
        assertThatThrownBy(() -> PartageImmobilierCalculator.calculate(
                "ALLEMAGNE", bd("300000"), bd("0"), bd("0.50"), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BigDecimal bd(String v) { return new BigDecimal(v); }
}
