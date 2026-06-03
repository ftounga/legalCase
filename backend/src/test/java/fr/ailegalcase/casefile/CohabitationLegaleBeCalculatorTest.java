package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.ailegalcase.casefile.CohabitationLegaleBeCalculator.CohabitationLegaleBeVerdict;
import fr.ailegalcase.casefile.CohabitationLegaleBeCalculator.ModeDissolutionCohabitationLegaleBe;
import fr.ailegalcase.casefile.CohabitationLegaleBeCalculator.VueCohabitationLegaleBe;

/**
 * SF-223-01 : tests unitaires du moteur décisionnel BE de la cohabitation
 * légale (3 vues × verdicts + formation impossible + dissolution unilatérale +
 * gates).
 */
class CohabitationLegaleBeCalculatorTest {

    private static CohabitationLegaleBeInput formation(boolean nonMaries, boolean capacite, boolean nonLie) {
        return new CohabitationLegaleBeInput(
                VueCohabitationLegaleBe.FORMATION, nonMaries, capacite, nonLie,
                true, null, null, null);
    }

    @Test
    void formation_conditions_reunies_valide() {
        CohabitationLegaleBeResult r =
                CohabitationLegaleBeCalculator.compute(formation(true, true, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(CohabitationLegaleBeVerdict.FORMATION_VALIDE);
        assertThat(r.conditions()).isEmpty();
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("officier"));
    }

    @Test
    void formation_condition_manquante_impossible_avec_motif() {
        CohabitationLegaleBeResult r =
                CohabitationLegaleBeCalculator.compute(formation(false, true, true), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(CohabitationLegaleBeVerdict.FORMATION_IMPOSSIBLE);
        assertThat(r.conditions()).isNotEmpty();
    }

    @Test
    void formation_toutes_conditions_manquantes_liste_3_motifs() {
        CohabitationLegaleBeResult r =
                CohabitationLegaleBeCalculator.compute(formation(false, false, false), "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(CohabitationLegaleBeVerdict.FORMATION_IMPOSSIBLE);
        assertThat(r.conditions()).hasSize(3);
    }

    @Test
    void effets_logement_familial_protege() {
        CohabitationLegaleBeInput in = new CohabitationLegaleBeInput(
                VueCohabitationLegaleBe.EFFETS, null, null, null,
                true, true, null, null);
        CohabitationLegaleBeResult r = CohabitationLegaleBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(CohabitationLegaleBeVerdict.EFFETS_QUALIFIES);
        assertThat(r.conditions()).anyMatch(c -> c.toLowerCase().contains("logement"));
        assertThat(r.conditions()).anyMatch(c -> c.toLowerCase().contains("charges"));
    }

    @Test
    void dissolution_unilaterale_exige_signification_huissier() {
        CohabitationLegaleBeInput in = new CohabitationLegaleBeInput(
                VueCohabitationLegaleBe.DISSOLUTION, null, null, null,
                null, null, ModeDissolutionCohabitationLegaleBe.DECLARATION_UNILATERALE, null);
        CohabitationLegaleBeResult r = CohabitationLegaleBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(CohabitationLegaleBeVerdict.DISSOLUTION_QUALIFIEE);
        assertThat(r.actesAProduire()).anyMatch(a -> a.toLowerCase().contains("huissier"));
    }

    @Test
    void dissolution_commune_pas_de_huissier() {
        CohabitationLegaleBeInput in = new CohabitationLegaleBeInput(
                VueCohabitationLegaleBe.DISSOLUTION, null, null, null,
                null, null, ModeDissolutionCohabitationLegaleBe.DECLARATION_COMMUNE, null);
        CohabitationLegaleBeResult r = CohabitationLegaleBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(CohabitationLegaleBeVerdict.DISSOLUTION_QUALIFIEE);
        assertThat(r.actesAProduire()).noneMatch(a -> a.toLowerCase().contains("huissier"));
    }

    @Test
    void dissolution_mariage_de_plein_droit() {
        CohabitationLegaleBeInput in = new CohabitationLegaleBeInput(
                VueCohabitationLegaleBe.DISSOLUTION, null, null, null,
                null, null, ModeDissolutionCohabitationLegaleBe.MARIAGE, null);
        CohabitationLegaleBeResult r = CohabitationLegaleBeCalculator.compute(in, "BELGIQUE");
        assertThat(r.verdict()).isEqualTo(CohabitationLegaleBeVerdict.DISSOLUTION_QUALIFIEE);
        assertThat(r.messages()).anyMatch(m -> m.toLowerCase().contains("plein droit"));
    }

    @Test
    void dissolution_sans_mode_leve_400() {
        CohabitationLegaleBeInput in = new CohabitationLegaleBeInput(
                VueCohabitationLegaleBe.DISSOLUTION, null, null, null,
                null, null, null, null);
        assertThatThrownBy(() -> CohabitationLegaleBeCalculator.compute(in, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void gate_pays_non_belgique_leve_exception() {
        assertThatThrownBy(() ->
                CohabitationLegaleBeCalculator.compute(formation(true, true, true), "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void bases_juridiques_pas_de_citation_jurisprudence() {
        CohabitationLegaleBeResult r =
                CohabitationLegaleBeCalculator.compute(formation(true, true, true), "BELGIQUE");
        assertThat(r.basesJuridiques()).isNotEmpty();
        // F-JU-04 parké — aucune citation jurisprudentielle (ECLI / arrêt) attendue.
        assertThat(r.basesJuridiques()).noneMatch(b -> b.toUpperCase().contains("ECLI"));
    }
}
