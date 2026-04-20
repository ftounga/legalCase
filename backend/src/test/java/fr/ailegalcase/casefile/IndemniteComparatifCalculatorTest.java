package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndemniteComparatifCalculatorTest {

    /** SF-139-01 : helper résolvant les fixtures DB-like selon country. */
    private static IndemniteComparatifResult calc(
            String country, String typeRupture, int anciennete, int age, BigDecimal salaire) {
        IndemniteBareme macron = "FRANCE".equals(country) ? TestIndemniteBaremes.macron(anciennete) : null;
        var cct = "BELGIQUE".equals(country) ? TestIndemniteBaremes.CCT_109 : null;
        return IndemniteComparatifCalculator.calculate(country, typeRupture, anciennete, age, salaire, macron, cct);
    }

    @Test
    void france_10ans_35ans_returnsValidResult() {
        IndemniteComparatifResult r = calc("FRANCE", "LICENCIEMENT", 10, 35, new BigDecimal("3000"));

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
        IndemniteComparatifResult r = calc("FRANCE", "LICENCIEMENT", 15, 55, new BigDecimal("4000"));
        assertThat(r.commentaire()).contains("50 ans");
    }

    @Test
    void belgique_5ans_returnsValidResult() {
        IndemniteComparatifResult r = calc("BELGIQUE", "LICENCIEMENT_ORDINAIRE", 5, 40, new BigDecimal("2500"));

        assertThat(r.country()).isEqualTo("BELGIQUE");
        assertThat(r.displayMode()).isEqualTo("CCT_109");
        assertThat(r.fourchetteBasseMois()).isPositive();
        assertThat(r.fourhetteHauteMois()).isGreaterThanOrEqualTo(r.fourchetteBasseMois());
        assertThat(r.baremeSource()).contains("CCT");
    }

    @Test
    void belgique_highAnciennete_commentaire() {
        IndemniteComparatifResult r = calc("BELGIQUE", "LICENCIEMENT_ORDINAIRE", 12, 45, new BigDecimal("3000"));
        assertThat(r.commentaire()).contains("Ancienneté significative");
    }

    @Test
    void montantsCalculated_correctly() {
        IndemniteComparatifResult r = calc("FRANCE", "LICENCIEMENT", 5, 30, new BigDecimal("2000"));
        assertThat(r.fourchetteBasseMontant())
                .isEqualByComparingTo(r.salaireMensuel().multiply(r.fourchetteBasseMois()).setScale(2, java.math.RoundingMode.HALF_UP));
    }

    @Test
    void invalidCountry_throws() {
        assertThatThrownBy(() -> calc("ALLEMAGNE", "LICENCIEMENT", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void france_licenciementEconomique_returnsMacronWithMessage() {
        IndemniteComparatifResult r = calc("FRANCE", "LICENCIEMENT_ECONOMIQUE", 8, 40, new BigDecimal("3500"));
        assertThat(r.displayMode()).isEqualTo("MACRON");
        assertThat(r.typeRupture()).isEqualTo("LICENCIEMENT_ECONOMIQUE");
        assertThat(r.contextualMessages()).isNotEmpty();
        assertThat(r.contextualMessages().get(0)).contains("économique");
    }

    @Test
    void france_ruptureConventionnelle_throws() {
        assertThatThrownBy(() -> calc("FRANCE", "RUPTURE_CONVENTIONNELLE", 12, 40, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalide pour FRANCE");
    }

    @Test
    void belgique_ruptureAmiable_throws() {
        assertThatThrownBy(() -> calc("BELGIQUE", "RUPTURE_AMIABLE", 5, 35, new BigDecimal("2500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalide pour BELGIQUE");
    }

    @Test
    void france_typeRupture_belge_throws() {
        assertThatThrownBy(() -> calc("FRANCE", "LICENCIEMENT_ORDINAIRE", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalide pour FRANCE");
    }

    @Test
    void belgique_typeRupture_fr_throws() {
        assertThatThrownBy(() -> calc("BELGIQUE", "RUPTURE_CONVENTIONNELLE", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("invalide pour BELGIQUE");
    }

    @Test
    void typeRupture_blank_throws() {
        assertThatThrownBy(() -> calc("FRANCE", null, 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requis");
        assertThatThrownBy(() -> calc("FRANCE", "", 5, 30, new BigDecimal("3000")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void france_baremeNull_throws() {
        // Si LegalReferentialService.getBaremeMacron renvoie null (DB vide), le calc lève IllegalStateException
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "FRANCE", "LICENCIEMENT", 5, 30, new BigDecimal("3000"), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Barème Macron non trouvé");
    }

    @Test
    void belgique_cctNull_throws() {
        assertThatThrownBy(() -> IndemniteComparatifCalculator.calculate(
                "BELGIQUE", "LICENCIEMENT_ORDINAIRE", 5, 30, new BigDecimal("3000"), null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CCT 109 non trouvé");
    }
}
