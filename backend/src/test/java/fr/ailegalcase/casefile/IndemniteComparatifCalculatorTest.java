package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndemniteComparatifCalculatorTest {

    @Test
    void france_10ans_35ans_returnsValidResult() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "FRANCE", "LICENCIEMENT", 10, 35, new BigDecimal("3000"));

        assertThat(r.country()).isEqualTo("FRANCE");
        assertThat(r.typeRupture()).isEqualTo("LICENCIEMENT");
        assertThat(r.displayMode()).isEqualTo("MACRON");
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
                "FRANCE", "LICENCIEMENT", 15, 55, new BigDecimal("4000"));
        assertThat(r.commentaire()).contains("50 ans");
    }

    @Test
    void belgique_5ans_returnsValidResult() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "BELGIQUE", "LICENCIEMENT_ORDINAIRE", 5, 40, new BigDecimal("2500"));

        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.displayMode()).isEqualTo("CCT_109");
        assertThat(r.fourchetteBasseMois()).isPositive();
        assertThat(r.fourhetteHauteMois()).isGreaterThanOrEqualTo(r.fourchetteBasseMois());
        assertThat(r.baremeSource()).contains("CCT");
    }

    @Test
    void belgique_highAnciennete_commentaire() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "BELGIQUE", "LICENCIEMENT_ORDINAIRE", 12, 45, new BigDecimal("3000"));
        assertThat(r.commentaire()).contains("Ancienneté significative");
    }

    @Test
    void montantsCalculated_correctly() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "FRANCE", "LICENCIEMENT", 5, 30, new BigDecimal("2000"));
        assertThat(r.fourchetteBasseMontant())
                .isEqualByComparingTo(r.salaireMensuel().multiply(r.fourchetteBasseMois()).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void invalidCountry_throws() {
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "ALLEMAGNE", "LICENCIEMENT", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // SF-DT-09-04 — type_rupture

    @Test
    void france_licenciementEconomique_returnsMacronWithMessage() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "FRANCE", "LICENCIEMENT_ECONOMIQUE", 8, 40, new BigDecimal("3500"));
        assertThat(r.displayMode()).isEqualTo("MACRON");
        assertThat(r.typeRupture()).isEqualTo("LICENCIEMENT_ECONOMIQUE");
        assertThat(r.contextualMessages()).isNotEmpty();
        assertThat(r.contextualMessages().get(0)).contains("économique");
    }

    @Test
    void france_ruptureConventionnelle_returnsIndemniteLegale() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "FRANCE", "RUPTURE_CONVENTIONNELLE", 12, 40, new BigDecimal("3000"));
        assertThat(r.displayMode()).isEqualTo("INDEMNITE_SPECIFIQUE");
        assertThat(r.indemniteLegaleMontant()).isNotNull();
        // 12 ans : 10×0.25 + 2×(1/3) = 2.5 + 0.666... = 3.1666...
        // × 3000 = 9500 (exact avec la précision 4 du calcul interne)
        assertThat(r.indemniteLegaleMontant())
                .isEqualByComparingTo(new BigDecimal("9500.00"));
        assertThat(r.contextualMessages()).anyMatch(m -> m.contains("L1237-13"));
    }

    @Test
    void belgique_ruptureAmiable_returnsNegociationLibre() {
        IndemniteComparatifResult r = IndemniteComparatifCalculator.calculate(
                "BELGIQUE", "RUPTURE_AMIABLE", 5, 35, new BigDecimal("2500"));
        assertThat(r.displayMode()).isEqualTo("NEGOCIATION_LIBRE");
        assertThat(r.indemniteLegaleMontant()).isNull();
        assertThat(r.fourchetteBasseMontant()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(r.contextualMessages()).anyMatch(m -> m.contains("Aucun barème"));
    }

    @Test
    void france_typeRupture_belge_throws() {
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "FRANCE", "LICENCIEMENT_ORDINAIRE", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalide pour FRANCE");
    }

    @Test
    void belgique_typeRupture_fr_throws() {
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "BELGIQUE", "RUPTURE_CONVENTIONNELLE", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalide pour BELGIQUE");
    }

    @Test
    void typeRupture_blank_throws() {
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "FRANCE", null, 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requis");
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "FRANCE", "", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void indemniteLegale_lessThanOneYear_returnsZero() {
        assertThat(IndemniteComparatifCalculator.computeIndemniteLegaleLicenciement(0, new BigDecimal("3000")))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void indemniteLegale_10ansPile() {
        // 10 ans × 0.25 × 3000 = 7500
        assertThat(IndemniteComparatifCalculator.computeIndemniteLegaleLicenciement(10, new BigDecimal("3000")))
                .isEqualByComparingTo(new BigDecimal("7500.00"));
    }

    @Test
    void indemniteLegale_15ans() {
        // 10 × 0.25 + 5 × (1/3) = 2.5 + 1.6666... = 4.1666...
        // × 3000 = 12500 (exact)
        assertThat(IndemniteComparatifCalculator.computeIndemniteLegaleLicenciement(15, new BigDecimal("3000")))
                .isEqualByComparingTo(new BigDecimal("12500.00"));
    }
}
