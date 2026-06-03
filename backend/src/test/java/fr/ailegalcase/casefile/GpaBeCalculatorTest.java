package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.GpaBeCalculator.GpaBeVerdict;
import fr.ailegalcase.casefile.GpaBeCalculator.LieuGpa;
import fr.ailegalcase.casefile.GpaBeCalculator.LienGenetique;

/**
 * SF-223-04 : tests unitaires du moteur décisionnel BE de cadrage de la
 * filiation post-GPA — 4 branches de filiation + message d'inopposabilité de la
 * convention + gates.
 */
class GpaBeCalculatorTest {

    private static GpaBeInput input(LieuGpa lieu, LienGenetique lien, Boolean acteEtranger) {
        return new GpaBeInput(lieu, lien, acteEtranger, true, true, true, null);
    }

    @Test
    void pere_intentionnel_avec_lien_genetique_reconnaissance() {
        GpaBeResult r = GpaBeCalculator.compute(
                input(LieuGpa.BELGIQUE, LienGenetique.PERE_INTENTIONNEL, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(GpaBeVerdict.FILIATION_PAR_RECONNAISSANCE);
        assertThat(r.cheminContentieux()).anyMatch(c -> c.toLowerCase().contains("reconnaissance"));
    }

    @Test
    void les_deux_lien_genetique_reconnaissance() {
        GpaBeResult r = GpaBeCalculator.compute(
                input(LieuGpa.BELGIQUE, LienGenetique.LES_DEUX, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(GpaBeVerdict.FILIATION_PAR_RECONNAISSANCE);
    }

    @Test
    void aucun_lien_genetique_adoption_post_naissance() {
        GpaBeResult r = GpaBeCalculator.compute(
                input(LieuGpa.BELGIQUE, LienGenetique.AUCUN, null), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(GpaBeVerdict.FILIATION_PAR_ADOPTION_POST_NAISSANCE);
        // Renvoi explicite vers adoption-be.
        assertThat(r.cheminContentieux()).anyMatch(c -> c.toLowerCase().contains("adoption-be"));
    }

    @Test
    void gpa_etranger_avec_acte_reconnaissance_acte_etranger() {
        GpaBeResult r = GpaBeCalculator.compute(
                input(LieuGpa.ETRANGER, LienGenetique.PERE_INTENTIONNEL, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(GpaBeVerdict.RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE);
        // Renvoi DIP.
        assertThat(r.cheminContentieux()).anyMatch(c -> c.toLowerCase().contains("dip")
                || c.toLowerCase().contains("reconnaissance de l'acte")
                || c.toLowerCase().contains("loi applicable"));
    }

    @Test
    void gpa_etranger_sans_acte_etranger_suit_lien_genetique() {
        // Étranger MAIS acte étranger non établi → on retombe sur l'arbre lien génétique.
        GpaBeResult r = GpaBeCalculator.compute(
                input(LieuGpa.ETRANGER, LienGenetique.AUCUN, false), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(GpaBeVerdict.FILIATION_PAR_ADOPTION_POST_NAISSANCE);
    }

    @Test
    void message_inopposabilite_convention_et_mater_semper_certa() {
        GpaBeResult r = GpaBeCalculator.compute(
                input(LieuGpa.BELGIQUE, LienGenetique.PERE_INTENTIONNEL, null), "BELGIQUE");
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("n'est pas opposable"));
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("mater semper certa"));
    }

    @Test
    void lieu_absent_leve_400() {
        GpaBeInput in = new GpaBeInput(
                null, LienGenetique.PERE_INTENTIONNEL, null, true, true, true, null);
        assertThatThrownBy(() -> GpaBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lien_genetique_absent_leve_400() {
        GpaBeInput in = new GpaBeInput(
                LieuGpa.BELGIQUE, null, null, true, true, true, null);
        assertThatThrownBy(() -> GpaBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void commentaire_trop_long_leve_400() {
        GpaBeInput in = new GpaBeInput(
                LieuGpa.BELGIQUE, LienGenetique.PERE_INTENTIONNEL, null, true, true, true,
                "x".repeat(1001));
        assertThatThrownBy(() -> GpaBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() -> GpaBeCalculator.compute(
                input(LieuGpa.BELGIQUE, LienGenetique.PERE_INTENTIONNEL, null), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        GpaBeResult r = GpaBeCalculator.compute(
                input(LieuGpa.BELGIQUE, LienGenetique.PERE_INTENTIONNEL, null), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
        assertThat(r.cheminContentieux()).noneMatch(c -> c.toUpperCase().contains("ECLI"));
    }
}
