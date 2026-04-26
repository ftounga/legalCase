package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.DevolutionLegaleCalculator.HeritierDesigne;
import fr.ailegalcase.casefile.DevolutionLegaleCalculator.ModaliteHeritier;
import fr.ailegalcase.casefile.DevolutionLegaleCalculator.OptionConjoint;
import fr.ailegalcase.casefile.DevolutionLegaleCalculator.OrdreActif;
import fr.ailegalcase.casefile.DevolutionLegaleCalculator.QualiteHeritier;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DevolutionLegaleCalculatorTest {

    private static final String FR = "FRANCE";

    // ============ Ordre 1 — Descendants seuls ============

    @Test
    void ordre1_3enfants_sansConjoint_chacun33pct() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 3, false, 0, 0,
                false, false, 0, 0, false, false, null, FR);
        assertThat(r.ordreActif()).isEqualTo(OrdreActif.DESCENDANTS);
        assertThat(r.heritiersDesignes()).hasSize(3);
        assertThat(r.heritiersDesignes()).allMatch(h ->
                h.qualite() == QualiteHeritier.DESCENDANT && h.ordre() == 1);
        assertThat(r.heritiersDesignes().get(0).quotePartPct())
                .isCloseTo(33.333, org.assertj.core.data.Offset.offset(0.01));
        assertThat(r.quotePartConjoint()).isEqualTo(0.0);
    }

    @Test
    void ordre1_1enfant_seul_100pct() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 1, false, 0, 0,
                false, false, 0, 0, false, false, null, FR);
        assertThat(r.heritiersDesignes()).hasSize(1);
        assertThat(r.heritiersDesignes().get(0).quotePartPct()).isEqualTo(100.0);
    }

    // ============ Ordre 1 — Descendants tous communs + conjoint ============

    @Test
    void ordre1_2enfantsTousCommuns_conjointQuart_conjoint25_enfants375() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                true, 2, true, 0, 0,
                false, false, 0, 0, false, false,
                OptionConjoint.QUART, FR);
        assertThat(r.ordreActif()).isEqualTo(OrdreActif.DESCENDANTS);
        assertThat(r.quotePartConjoint()).isEqualTo(25.0);
        assertThat(r.modaliteConjoint()).isEqualTo(ModaliteHeritier.PLEINE_PROPRIETE);
        assertThat(r.heritiersDesignes()).hasSize(3); // conjoint + 2 enfants
        long enfants = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.DESCENDANT)
                .count();
        assertThat(enfants).isEqualTo(2);
        r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.DESCENDANT)
                .forEach(h -> assertThat(h.quotePartPct()).isEqualTo(37.5));
    }

    @Test
    void ordre1_2enfantsTousCommuns_conjointUsufruit_conjoint100US_enfants100NP() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                true, 2, true, 0, 0,
                false, false, 0, 0, false, false,
                OptionConjoint.USUFRUIT, FR);
        assertThat(r.quotePartConjoint()).isEqualTo(100.0);
        assertThat(r.modaliteConjoint()).isEqualTo(ModaliteHeritier.USUFRUIT);
        long npEnfants = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.DESCENDANT
                        && h.modalite() == ModaliteHeritier.NUE_PROPRIETE)
                .count();
        assertThat(npEnfants).isEqualTo(2);
    }

    // ============ Ordre 1 — Descendants NON communs (recomposée) ============

    @Test
    void ordre1_2enfantsNonCommuns_conjoint25Force_optionUsufruitIgnoree() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                true, 2, false, 0, 0,
                false, false, 0, 0, false, false,
                OptionConjoint.USUFRUIT, FR); // option usufruit fournie mais ignorée
        assertThat(r.quotePartConjoint()).isEqualTo(25.0);
        assertThat(r.modaliteConjoint()).isEqualTo(ModaliteHeritier.PLEINE_PROPRIETE);
        // Risque famille recomposée présent
        assertThat(r.risquesContentieux()).anyMatch(s -> s.contains("recomposée"));
    }

    // ============ Ordre 1 — Représentation ============

    @Test
    void ordre1_representation_1vivant_1predecedeAvec2petits() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 2, false, 1, 2,
                false, false, 0, 0, false, false, null, FR);
        assertThat(r.representationActive()).isTrue();
        // 2 souches de 50% chacune ; petits-enfants représentent 1 souche → 25% chacun
        long descVivants = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.DESCENDANT)
                .count();
        long representants = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.REPRESENTANT)
                .count();
        assertThat(descVivants).isEqualTo(1);
        assertThat(representants).isEqualTo(2);
        // Vérifier les quotes-parts
        r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.DESCENDANT)
                .forEach(h -> assertThat(h.quotePartPct()).isEqualTo(50.0));
        r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.REPRESENTANT)
                .forEach(h -> assertThat(h.quotePartPct()).isEqualTo(25.0));
    }

    // ============ Ordre 2 — Conjoint + parents ============

    @Test
    void ordre2_2parents_2freres_conjoint_conjoint50_parents25_freresExclus() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                true, 0, false, 0, 0,
                true, true, 2, 0, false, false, null, FR);
        assertThat(r.ordreActif()).isEqualTo(OrdreActif.PRIVILEGIES);
        assertThat(r.quotePartConjoint()).isEqualTo(50.0);
        // Parents sont là, frères exclus
        long parents = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.PERE
                        || h.qualite() == QualiteHeritier.MERE)
                .count();
        assertThat(parents).isEqualTo(2);
        long freres = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.FRERE_SOEUR)
                .count();
        assertThat(freres).isEqualTo(0);
    }

    @Test
    void ordre2_1parent_conjoint_conjoint75_parent25() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                true, 0, false, 0, 0,
                true, false, 0, 0, false, false, null, FR);
        assertThat(r.quotePartConjoint()).isEqualTo(75.0);
        assertThat(r.heritiersDesignes()).anyMatch(h -> h.qualite() == QualiteHeritier.PERE
                && h.quotePartPct() == 25.0);
    }

    // ============ Ordre 2 — Conjoint sans parents ni descendants ============

    @Test
    void ordre2_conjointSeul_3freres_conjoint100_freresExclus() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                true, 0, false, 0, 0,
                false, false, 3, 0, false, false, null, FR);
        // Pas descendants ni parents → conjoint exclut les frères (757-2)
        assertThat(r.quotePartConjoint()).isEqualTo(100.0);
        assertThat(r.heritiersDesignes()).hasSize(1);
        assertThat(r.heritiersDesignes().get(0).qualite()).isEqualTo(QualiteHeritier.CONJOINT);
        assertThat(r.risquesContentieux()).anyMatch(s -> s.contains("757-3"));
    }

    @Test
    void ordre2_conjointSeul_aucunAutre_conjoint100() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                true, 0, false, 0, 0,
                false, false, 0, 0, false, false, null, FR);
        assertThat(r.ordreActif()).isEqualTo(OrdreActif.CONJOINT_SEUL);
        assertThat(r.quotePartConjoint()).isEqualTo(100.0);
    }

    // ============ Ordre 2 sans conjoint ============

    @Test
    void ordre2_sansConjoint_2parents_4freres_parents50_freres50() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 0, false, 0, 0,
                true, true, 4, 0, false, false, null, FR);
        // 2 parents (25% chacun) + 4 frères (12.5% chacun)
        long parents = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.PERE
                        || h.qualite() == QualiteHeritier.MERE)
                .count();
        assertThat(parents).isEqualTo(2);
        long freres = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.FRERE_SOEUR)
                .count();
        assertThat(freres).isEqualTo(4);
        r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.FRERE_SOEUR)
                .forEach(h -> assertThat(h.quotePartPct()).isEqualTo(12.5));
    }

    @Test
    void ordre2_sansConjoint_freresSeuls_4freres_25pctChacun() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 0, false, 0, 0,
                false, false, 4, 0, false, false, null, FR);
        long freres = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.FRERE_SOEUR)
                .count();
        assertThat(freres).isEqualTo(4);
        r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.FRERE_SOEUR)
                .forEach(h -> assertThat(h.quotePartPct()).isEqualTo(25.0));
    }

    // ============ Ordre 3 — Ascendants ordinaires + fente ============

    @Test
    void ordre3_ascendantsOrdinaires_fenteActive_50_50() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 0, false, 0, 0,
                false, false, 0, 0, true, false, null, FR);
        assertThat(r.ordreActif()).isEqualTo(OrdreActif.ASCENDANTS_ORDINAIRES);
        assertThat(r.fenteApplicable()).isTrue();
        assertThat(r.heritiersDesignes()).hasSize(2);
        HeritierDesigne paternel = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.ASCENDANT_ORDINAIRE_PATERNEL)
                .findFirst().orElseThrow();
        HeritierDesigne maternel = r.heritiersDesignes().stream()
                .filter(h -> h.qualite() == QualiteHeritier.ASCENDANT_ORDINAIRE_MATERNEL)
                .findFirst().orElseThrow();
        assertThat(paternel.quotePartPct()).isEqualTo(50.0);
        assertThat(maternel.quotePartPct()).isEqualTo(50.0);
    }

    // ============ Ordre 4 — Collatéraux ordinaires ============

    @Test
    void ordre4_collaterauxOrdinairesUniquement_100pct() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 0, false, 0, 0,
                false, false, 0, 0, false, true, null, FR);
        assertThat(r.ordreActif()).isEqualTo(OrdreActif.COLLATERAUX_ORDINAIRES);
        assertThat(r.heritiersDesignes()).hasSize(1);
        assertThat(r.heritiersDesignes().get(0).qualite())
                .isEqualTo(QualiteHeritier.COLLATERAL_ORDINAIRE);
        assertThat(r.heritiersDesignes().get(0).quotePartPct()).isEqualTo(100.0);
    }

    // ============ Déshérence ============

    @Test
    void aucunHeritier_desherence_messagesEtRisques() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 0, false, 0, 0,
                false, false, 0, 0, false, false, null, FR);
        assertThat(r.ordreActif()).isEqualTo(OrdreActif.DESHERENCE);
        assertThat(r.heritiersDesignes()).isEmpty();
        assertThat(r.messages()).anyMatch(m -> m.toUpperCase().contains("DÉSHÉRENCE"));
        assertThat(r.risquesContentieux()).isNotEmpty();
    }

    // ============ Validations ============

    @Test
    void validation_nbDescendantsNegatif_throws() {
        assertThatThrownBy(() -> DevolutionLegaleCalculator.compute(
                false, -1, false, 0, 0,
                false, false, 0, 0, false, false, null, FR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("descendants");
    }

    @Test
    void validation_freresPredecedesSuperieurs_throws() {
        assertThatThrownBy(() -> DevolutionLegaleCalculator.compute(
                false, 0, false, 0, 0,
                false, false, 2, 5, false, false, null, FR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("excéder");
    }

    @Test
    void validation_optionConjointManquante_avecConjointTousCommuns_throws() {
        assertThatThrownBy(() -> DevolutionLegaleCalculator.compute(
                true, 2, true, 0, 0,
                false, false, 0, 0, false, false,
                null, // option manquante
                FR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Option");
    }

    @Test
    void validation_country_null_throws() {
        assertThatThrownBy(() -> DevolutionLegaleCalculator.compute(
                false, 1, false, 0, 0,
                false, false, 0, 0, false, false, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pays");
    }

    @Test
    void validation_country_BELGIQUE_throws_mentionneFeatureJumelle() {
        assertThatThrownBy(() -> DevolutionLegaleCalculator.compute(
                false, 1, false, 0, 0,
                false, false, 0, 0, false, false, null, "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("F-FA-24-BE");
    }

    // ============ Base juridique + formule ============

    @Test
    void baseJuridique_contient_731_734_738_757_746() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 1, false, 0, 0,
                false, false, 0, 0, false, false, null, FR);
        assertThat(r.baseJuridique()).contains("731");
        assertThat(r.baseJuridique()).contains("734");
        assertThat(r.baseJuridique()).contains("738");
        assertThat(r.baseJuridique()).contains("757");
        assertThat(r.baseJuridique()).contains("746");
    }

    @Test
    void formule_contient_ordreActif_et_score() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 1, false, 0, 0,
                false, false, 0, 0, false, false, null, FR);
        assertThat(r.formule()).contains("DESCENDANTS");
        assertThat(r.formule()).contains("score");
    }

    @Test
    void country_lowercase_normalized() {
        DevolutionLegaleResult r = DevolutionLegaleCalculator.compute(
                false, 1, false, 0, 0,
                false, false, 0, 0, false, false, null, "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }
}
