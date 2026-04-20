package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AncienneteCalculatorTest {

    @Test
    void metallurgie_10ans_congesEtPrimeCorrects() {
        AncienneteResult result = AncienneteCalculator.calculate(
                "METALLURGIE",
                LocalDate.now().minusYears(10).minusMonths(3),
                new BigDecimal("3000"),
                25,
                new BigDecimal("5"),
                TestConventionBaremes.METALLURGIE
        );

        assertThat(result.ancienneteAnnees()).isEqualTo(10);
        assertThat(result.congesLegauxJours()).isEqualTo(25);
        assertThat(result.congesSupplementairesJours()).isEqualTo(2); // +2 après 10 ans
        assertThat(result.congesTotalJours()).isEqualTo(27);
        assertThat(result.primeAnciennetePourcentage()).isEqualByComparingTo(new BigDecimal("9")); // 9% après 9 ans
    }

    @Test
    void metallurgie_10ans_ecartConges() {
        AncienneteResult result = AncienneteCalculator.calculate(
                "METALLURGIE",
                LocalDate.now().minusYears(10),
                new BigDecimal("3000"),
                25, // contrat: 25 jours, minimum: 27
                BigDecimal.ZERO,
                TestConventionBaremes.METALLURGIE
        );

        assertThat(result.ecarts()).anyMatch(e ->
                "Congés annuels".equals(e.champ()) && "ECART".equals(e.verdict()));
    }

    @Test
    void cp200_belgique_5ans() {
        AncienneteResult result = AncienneteCalculator.calculate(
                "CP200",
                LocalDate.now().minusYears(5).minusMonths(1),
                new BigDecimal("2500"),
                21,
                new BigDecimal("2"),
                TestConventionBaremes.CP200
        );

        assertThat(result.country()).isEqualTo("BELGIQUE");
        assertThat(result.congesLegauxJours()).isEqualTo(20);
        assertThat(result.congesSupplementairesJours()).isEqualTo(1); // +1 après 5 ans
        assertThat(result.congesTotalJours()).isEqualTo(21);
        assertThat(result.primeAnciennetePourcentage()).isEqualByComparingTo(new BigDecimal("2")); // 2% après 5 ans
    }

    @Test
    void cp200_congesConformes() {
        AncienneteResult result = AncienneteCalculator.calculate(
                "CP200",
                LocalDate.now().minusYears(5),
                new BigDecimal("2500"),
                21,
                new BigDecimal("2"),
                TestConventionBaremes.CP200
        );

        assertThat(result.ecarts()).anyMatch(e ->
                "Congés annuels".equals(e.champ()) && "CONFORME".equals(e.verdict()));
    }

    @Test
    void primeMontantCalculation() {
        AncienneteResult result = AncienneteCalculator.calculate(
                "SYNTEC",
                LocalDate.now().minusYears(5),
                new BigDecimal("4000"),
                25,
                BigDecimal.ZERO,
                TestConventionBaremes.SYNTEC
        );

        // Syntec 5 ans = 5%, 4000 * 5% = 200
        assertThat(result.primeAnciennetePourcentage()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(result.primeAncienneteMontant()).isEqualByComparingTo(new BigDecimal("200.00"));
    }

    @Test
    void ecartPrimeDetected() {
        AncienneteResult result = AncienneteCalculator.calculate(
                "SYNTEC",
                LocalDate.now().minusYears(10),
                new BigDecimal("4000"),
                30,
                new BigDecimal("3"), // contrat 3% mais convention = 10%
                TestConventionBaremes.SYNTEC
        );

        assertThat(result.ecarts()).anyMatch(e ->
                "Prime d'ancienneté".equals(e.champ()) && "ECART".equals(e.verdict()));
    }

    @Test
    void nouvelEmbauche_pasDePrimeNiCongesSupp() {
        AncienneteResult result = AncienneteCalculator.calculate(
                "METALLURGIE",
                LocalDate.now().minusMonths(6),
                new BigDecimal("2500"),
                25,
                BigDecimal.ZERO,
                TestConventionBaremes.METALLURGIE
        );

        assertThat(result.ancienneteAnnees()).isZero();
        assertThat(result.congesSupplementairesJours()).isZero();
        assertThat(result.primeAnciennetePourcentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void conventionInconnue_throwsException() {
        assertThatThrownBy(() -> AncienneteCalculator.calculate(
                "INCONNU", LocalDate.now().minusYears(5),
                new BigDecimal("3000"), 25, BigDecimal.ZERO,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Convention inconnue");
    }

    @Test
    void SF_DT_07_05_primeContratSuperieureBareme_appliqueContrat() {
        // BTP à 15 ans : bareme = 12% (référentiel). User 22% → effectif = 22%, pas d'écart.
        AncienneteResult result = AncienneteCalculator.calculate(
                "BTP", LocalDate.now().minusYears(15),
                new BigDecimal("3000"), 25, new BigDecimal("22"),
                TestConventionBaremes.BTP
        );
        assertThat(result.primeAnciennetePourcentage()).isEqualByComparingTo(new BigDecimal("22"));
        assertThat(result.primeAncienneteMontant()).isEqualByComparingTo(new BigDecimal("660"));
        AncienneteResult.Ecart primeEcart = result.ecarts().stream()
                .filter(e -> e.champ().equals("Prime d'ancienneté")).findFirst().orElseThrow();
        assertThat(primeEcart.verdict()).isEqualTo(AncienneteResult.Ecart.CONFORME);
    }

    @Test
    void SF_DT_07_05_primeContratInferieureBareme_appliqueBaremeAvecEcart() {
        // BTP 15 ans : bareme = 12%. User 5% → effectif = 12%, écart détecté.
        AncienneteResult result = AncienneteCalculator.calculate(
                "BTP", LocalDate.now().minusYears(15),
                new BigDecimal("3000"), 25, new BigDecimal("5"),
                TestConventionBaremes.BTP
        );
        assertThat(result.primeAnciennetePourcentage()).isEqualByComparingTo(new BigDecimal("12"));
        assertThat(result.primeAncienneteMontant()).isEqualByComparingTo(new BigDecimal("360"));
        AncienneteResult.Ecart primeEcart = result.ecarts().stream()
                .filter(e -> e.champ().equals("Prime d'ancienneté")).findFirst().orElseThrow();
        assertThat(primeEcart.verdict()).isEqualTo(AncienneteResult.Ecart.ECART);
        assertThat(primeEcart.attendu()).contains("12");
        assertThat(primeEcart.contractuel()).contains("5");
    }

    @Test
    void SF_DT_07_05_congesContratSuperieurBareme_appliqueContrat() {
        // SYNTEC : bareme = 25j légaux. User 30 → effectif = 30, conforme.
        AncienneteResult result = AncienneteCalculator.calculate(
                "SYNTEC", LocalDate.now().minusYears(2),
                new BigDecimal("3000"), 30, BigDecimal.ZERO,
                TestConventionBaremes.SYNTEC
        );
        assertThat(result.congesTotalJours()).isEqualTo(30);
        AncienneteResult.Ecart congesEcart = result.ecarts().stream()
                .filter(e -> e.champ().equals("Congés annuels")).findFirst().orElseThrow();
        assertThat(congesEcart.verdict()).isEqualTo(AncienneteResult.Ecart.CONFORME);
    }

    @Test
    void allConventions_calculateWithoutError() {
        for (ConventionBareme b : TestConventionBaremes.SAMPLE) {
            AncienneteResult result = AncienneteCalculator.calculate(
                    b.code(),
                    LocalDate.now().minusYears(8),
                    new BigDecimal("3000"),
                    25,
                    new BigDecimal("5"),
                    b
            );
            assertThat(result.conventionCode()).as("code %s", b.code()).isEqualTo(b.code());
            assertThat(result.congesTotalJours()).as("conges %s", b.code()).isPositive();
            assertThat(result.ecarts()).as("ecarts %s", b.code()).isNotEmpty();
        }
    }
}
