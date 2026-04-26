package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.AdoptionCalculator.FormeAdoption;
import fr.ailegalcase.casefile.AdoptionCalculator.VerdictRecevabilite;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AdoptionCalculatorTest {

    // ============ Verdict ELEVEE — Plénière valide ============

    @Test
    void plenierePleinementValide_returnsELEVEE_formePLENIERE() {
        // Couple marié 35 ans + enfant 5 ans → diff 30 ans, tous critères OK
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true,  // consentementParents
                false, // consentementAdopte (pas requis < 13 ans)
                true,  // consentementConjointAdoptant
                true,  // enquetes
                true,  // placement6mois
                false, // pupilleEtat
                true,  // adoptantMarie
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.PLENIERE);
        assertThat(r.differenceAgeAns()).isEqualTo(30);
        assertThat(r.criteresNonRemplis()).isEmpty();
    }

    // ============ Verdict ELEVEE — Simple valide ============

    @Test
    void simplePleinementValide_returnsELEVEE_formeSIMPLE() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                40, 20,
                false, // pas de parents nécessaire (adopté majeur)
                true,  // consentementAdopte
                false, // pas marié
                false,
                false,
                false,
                false, // pas marié
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.SIMPLE);
    }

    // ============ Bascule plénière → simple ============

    @Test
    void pleniereDemande_maisAdoptantTropJeune_basculeSIMPLE_siSimpleOk() {
        // Adoptant 27 ans, célibataire (< 28 requis pour plénière), mais OK pour simple (≥ 26)
        // Adopté mineur 10 ans → consentement parents requis
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                27, 10,
                true,  // consentementParents
                false, // pas requis < 13
                false,
                false,
                false,
                false,
                false, // célibataire
                "FRANCE");
        // Plénière KO (placement 6 mois manquant aussi)
        // Simple OK (26 ans ✓ + diff 17 ans ✓ + consentement parents ✓ + adopté < 13 donc consentement adopté pas requis)
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.SIMPLE);
        assertThat(r.formule()).contains("PLENIERE");
    }

    @Test
    void pleniereDemande_maisAdopteTropAge_basculeSIMPLE() {
        // Adopté 17 ans → trop âgé pour plénière (< 15 requis)
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                40, 17,
                true,
                true, // consentement adopté requis ≥ 13
                false,
                false,
                false,
                false,
                false,
                "FRANCE");
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.SIMPLE);
    }

    @Test
    void pleniereDemande_sansPlacement6mois_FAIBLE_etRecommandeSIMPLE() {
        // Plénière sans placement → cardinaux KO. Simple OK avec mêmes paramètres.
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true,
                false,
                true,
                true,
                false, // placement6mois manquant
                false,
                true,
                "FRANCE");
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.SIMPLE);
        assertThat(r.criteresNonRemplis()).isEmpty(); // bilan = simple, simple OK
    }

    // ============ Adoption simple ============

    @Test
    void simpleDemande_avecAdopteAdulte_resteELEVEE() {
        // Pas de limite d'âge en simple
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                50, 30,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.SIMPLE);
    }

    // ============ Différence d'âge ============

    @Test
    void differenceAgeInsuffisante_FAIBLE() {
        // Diff = 10 ans < 15 requis
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                30, 20,
                false,
                true,
                false,
                false,
                false,
                false,
                false,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.AUCUNE);
        assertThat(r.criteresNonRemplis()).anyMatch(s -> s.contains("Différence d'âge"));
    }

    // ============ Consentements ============

    @Test
    void consentementParentsManquant_FAIBLE() {
        // Adopté mineur, pas pupille, sans consentement parents
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                false, // consentementParents manquant
                false,
                true,
                true,
                true,
                false, // pas pupille
                true,
                "FRANCE");
        assertThat(r.criteresNonRemplis()).anyMatch(s -> s.toLowerCase().contains("consentement"));
    }

    @Test
    void consentementAdolescent13ans_obligatoire() {
        // Adopté 14 ans en simple sans son consentement
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                40, 14,
                true,
                false, // consentement adopté manquant
                false,
                false,
                false,
                false,
                false,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(s -> s.toLowerCase().contains("adopt"));
    }

    @Test
    void consentementConjointMarié_obligatoire() {
        // Adoptant marié sans consentement conjoint
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                40, 20,
                false,
                true,
                false, // consentement conjoint manquant
                false,
                false,
                false,
                true, // marié
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
        assertThat(r.criteresNonRemplis()).anyMatch(s -> s.toLowerCase().contains("conjoint"));
    }

    // ============ Critères secondaires ============

    @Test
    void enquetesNonRealisees_MOYENNE_carCritereSecondaire() {
        // Plénière complète sauf enquêtes (secondaire)
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true,
                false,
                true,
                false, // enquetes
                true,
                false,
                true,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.MOYENNE);
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.PLENIERE);
    }

    // ============ Pupille ============

    @Test
    void pupilleEtat_facilitePleniere() {
        // Pupille → consentement parents pas requis
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                false, // pas de consentement parents
                false,
                true,
                true,
                true,
                true, // pupille
                true,
                "FRANCE");
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.PLENIERE);
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.ELEVEE);
    }

    // ============ Adoptant trop jeune ============

    @Test
    void adoptantTresJeune_FAIBLE() {
        // 22 ans < 26 requis
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                22, 5,
                true,
                false,
                false,
                false,
                false,
                false,
                false,
                "FRANCE");
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    // ============ Country ============

    @Test
    void country_BELGIQUE_throws() {
        assertThatThrownBy(() -> AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true, false, true, true, true, false, true,
                "BELGIQUE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("BELGIQUE");
    }

    @Test
    void country_FRANCE_normalized_lowercase() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                40, 20,
                false, true, false, false, false, false, false,
                "france");
        assertThat(r.country()).isEqualTo("FRANCE");
    }

    // ============ Validations ============

    @Test
    void validation_formeAdoption_null_throws() {
        assertThatThrownBy(() -> AdoptionCalculator.compute(
                null,
                35, 5,
                true, false, true, true, true, false, true,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Forme");
    }

    @Test
    void validation_ageAdoptant_negative_throws() {
        assertThatThrownBy(() -> AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                -1, 5,
                true, false, true, true, true, false, true,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adoptant");
    }

    @Test
    void validation_ageAdopte_null_throws() {
        assertThatThrownBy(() -> AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                35, null,
                true, false, true, true, true, false, true,
                "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("adopté");
    }

    @Test
    void differenceAge_calculee_correctement() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                42, 17,
                true, true, false, false, false, false, false,
                "FRANCE");
        assertThat(r.differenceAgeAns()).isEqualTo(25);
    }

    // ============ Formule + messages ============

    @Test
    void formule_contient_verdict_et_forme() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true, false, true, true, true, false, true,
                "FRANCE");
        assertThat(r.formule()).contains("PLENIERE");
        assertThat(r.formule()).containsAnyOf("ELEVEE", "MOYENNE", "FAIBLE");
    }

    @Test
    void messages_mentionnent_articles_343_360() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true, false, true, true, true, false, true,
                "FRANCE");
        assertThat(r.messages()).anyMatch(m -> m.contains("343"));
        assertThat(r.messages()).anyMatch(m -> m.contains("360") || m.contains("370"));
    }

    @Test
    void documentsRequis_listeNonVide_quandPlenierePropose() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true, false, true, true, true, false, true,
                "FRANCE");
        assertThat(r.documentsRequis()).isNotEmpty();
        assertThat(r.documentsRequis()).anyMatch(d -> d.toLowerCase().contains("requête")
                || d.toLowerCase().contains("acte"));
    }

    @Test
    void risqueRefus_alimente_quandFaible() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                30, 20, // diff insuffisante
                false, true, false, false, false, false, false,
                "FRANCE");
        assertThat(r.risqueRefus()).isNotEmpty();
    }

    @Test
    void delaiInstructionMois_dans_fourchette_6_18() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                35, 5,
                true, false, true, true, true, false, true,
                "FRANCE");
        assertThat(r.delaiInstructionMois()).isBetween(6, 18);
    }

    @Test
    void aucuneFormeApplicable_returnsAUCUNE_etFAIBLE() {
        // Adoptant 22 ans + différence d'âge 0 (même âge) → tout casse
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.PLENIERE,
                22, 22,
                false, false, false, false, false, false, false,
                "FRANCE");
        assertThat(r.formeRecommandee()).isEqualTo(FormeAdoption.AUCUNE);
        assertThat(r.verdictRecevabilite()).isEqualTo(VerdictRecevabilite.FAIBLE);
    }

    @Test
    void baseJuridique_contient_343_et_370() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                40, 20,
                false, true, false, false, false, false, false,
                "FRANCE");
        assertThat(r.baseJuridique()).contains("343");
        assertThat(r.baseJuridique()).contains("370");
    }

    @Test
    void booleanNull_traitesCommeFalse() {
        AdoptionResult r = AdoptionCalculator.compute(
                FormeAdoption.SIMPLE,
                40, 20,
                null, null, null, null, null, null, null,
                "FRANCE");
        assertThat(r.consentementParents()).isFalse();
        assertThat(r.consentementAdopte()).isFalse();
        assertThat(r.consentementConjointAdoptant()).isFalse();
        assertThat(r.enquetes()).isFalse();
        assertThat(r.placement6mois()).isFalse();
        assertThat(r.pupilleEtat()).isFalse();
        assertThat(r.adoptantMarie()).isFalse();
    }
}
