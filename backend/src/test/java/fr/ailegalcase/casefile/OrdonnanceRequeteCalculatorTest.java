package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.OrdonnanceRequeteCalculator.CritereRecevabilite;
import fr.ailegalcase.casefile.OrdonnanceRequeteCalculator.MotifRequete;
import fr.ailegalcase.casefile.OrdonnanceRequeteCalculator.VerdictAccordeProbabilite;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrdonnanceRequeteCalculatorTest {

    // =============== Cas nominaux par motif ===============

    @Test
    void detournementPatrimoine_tousCriteresOK_returnsELEVEE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, false, "FRANCE");
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.ELEVEE);
        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.delaiTypiqueJoursMin()).isEqualTo(5);
        assertThat(r.delaiTypiqueJoursMax()).isEqualTo(15);
    }

    @Test
    void enlevementInternational_avecEnfants_tousCriteresOK_returnsELEVEE_delai_1_3() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ENLEVEMENT_INTERNATIONAL, true, true, true, true, "FRANCE");
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.ELEVEE);
        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.delaiTypiqueJoursMin()).isEqualTo(1);
        assertThat(r.delaiTypiqueJoursMax()).isEqualTo(3);
    }

    @Test
    void organisationInsolvabilite_tousCriteresOK_returnsELEVEE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ORGANISATION_INSOLVABILITE, true, true, true, false, "FRANCE");
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.ELEVEE);
        assertThat(r.delaiTypiqueJoursMin()).isEqualTo(5);
        assertThat(r.delaiTypiqueJoursMax()).isEqualTo(15);
        assertThat(r.baseJuridique()).contains("saisies conservatoires");
    }

    @Test
    void preuveAbandon_tousCriteresOK_returnsELEVEE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.PREUVE_ABANDON, true, true, true, false, "FRANCE");
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.ELEVEE);
    }

    @Test
    void accesPieces_tousCriteresOK_returnsELEVEE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ACCES_PIECES, true, true, true, false, "FRANCE");
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.ELEVEE);
        assertThat(r.baseJuridique()).contains("145 CPC");
    }

    // =============== Critères manquants ===============

    @Test
    void urgenceManquante_returnsMOYENNE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, false, true, true, false, "FRANCE");
        // 2 critères / 3 → score 66 → MOYENNE (la dérogation est OK donc pas FAIBLE forcé)
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.MOYENNE);
        assertThat(r.scoreEligibilite()).isEqualTo(66);
        assertThat(r.criteresManquants()).contains(CritereRecevabilite.URGENCE_JUSTIFIEE);
    }

    @Test
    void derogationContradictoireManquante_returnsFAIBLE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, false, true, false, "FRANCE");
        // Dérogation manquante → forçage FAIBLE (le juge renvoie en référé contradictoire)
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.FAIBLE);
        assertThat(r.criteresManquants()).contains(CritereRecevabilite.DEROGATION_CONTRADICTOIRE_JUSTIFIEE);
        assertThat(String.join("\n", r.messages())).contains("référé contradictoire");
    }

    @Test
    void pieceJustificativeManquante_returnsMOYENNE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, false, false, "FRANCE");
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.MOYENNE);
        assertThat(r.criteresManquants()).contains(CritereRecevabilite.PIECE_JUSTIFICATIVE_FOURNIE);
    }

    // =============== Verdicts (cohérence scoring) ===============

    @Test
    void aucunCritere_returnsFAIBLE_score0() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, false, false, false, false, "FRANCE");
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.FAIBLE);
        assertThat(r.scoreEligibilite()).isEqualTo(0);
        assertThat(r.criteresRemplis()).isEmpty();
        assertThat(r.criteresManquants()).hasSize(3);
    }

    @Test
    void deuxCriteresManquants_returnsFAIBLE() {
        // Urgence et pièce manquantes (mais dérogation OK)
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, false, true, false, false, "FRANCE");
        assertThat(r.scoreEligibilite()).isEqualTo(33);
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.FAIBLE);
    }

    @Test
    void unCritereManquant_returnsMOYENNE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, false, true, true, false, "FRANCE");
        assertThat(r.scoreEligibilite()).isEqualTo(66);
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.MOYENNE);
    }

    @Test
    void zeroCritereManquant_returnsELEVEE() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, false, "FRANCE");
        assertThat(r.scoreEligibilite()).isEqualTo(100);
        assertThat(r.verdictAccordeProbabilite()).isEqualTo(VerdictAccordeProbabilite.ELEVEE);
        assertThat(r.criteresManquants()).isEmpty();
    }

    // =============== Délais ===============

    @Test
    void delai_IST_via_motifEnlevement_meme_sans_enfants() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ENLEVEMENT_INTERNATIONAL, true, true, true, false, "FRANCE");
        assertThat(r.delaiTypiqueJoursMin()).isEqualTo(1);
        assertThat(r.delaiTypiqueJoursMax()).isEqualTo(3);
    }

    @Test
    void delai_IST_via_presenceEnfants_meme_si_motif_non_IST() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, true, "FRANCE");
        assertThat(r.delaiTypiqueJoursMin()).isEqualTo(1);
        assertThat(r.delaiTypiqueJoursMax()).isEqualTo(3);
    }

    @Test
    void delai_5_15_pour_motifs_non_IST_sans_enfants() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.PREUVE_ABANDON, true, true, true, false, "FRANCE");
        assertThat(r.delaiTypiqueJoursMin()).isEqualTo(5);
        assertThat(r.delaiTypiqueJoursMax()).isEqualTo(15);
    }

    // =============== Recours adverse ===============

    @Test
    void recoursAdverseDelai_15_jours_pour_tous_les_cas() {
        OrdonnanceRequeteResult r1 = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ACCES_PIECES, true, true, true, false, "FRANCE");
        OrdonnanceRequeteResult r2 = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, false, false, false, false, "FRANCE");
        assertThat(r1.recoursAdverseDelaiJours()).isEqualTo(15);
        assertThat(r2.recoursAdverseDelaiJours()).isEqualTo(15);
    }

    @Test
    void messages_mentionnent_recours_referee_retractation_15_jours() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.PREUVE_ABANDON, true, true, true, false, "FRANCE");
        assertThat(String.join("\n", r.messages()))
                .contains("référé-rétractation")
                .contains("15 jours");
    }

    // =============== Base juridique ===============

    @Test
    void baseJuridique_FR_motif_IST_contient_373_2_6_Cciv() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ENLEVEMENT_INTERNATIONAL, true, true, true, true, "FRANCE");
        assertThat(r.baseJuridique()).contains("373-2-6 Cciv");
        assertThat(r.baseJuridique()).contains("493");
        assertThat(r.baseJuridique()).contains("497");
    }

    @Test
    void baseJuridique_FR_motif_non_IST_sans_enfants_neContientPas_373_2_6() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, false, "FRANCE");
        assertThat(r.baseJuridique()).doesNotContain("373-2-6 Cciv");
        assertThat(r.baseJuridique()).contains("493");
    }

    @Test
    void baseJuridique_BE_mentionne_art_1025_CJ() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, false, "BELGIQUE");
        assertThat(r.baseJuridique()).contains("1025");
        assertThat(r.baseJuridique()).contains("CJ");
        assertThat(r.country()).isEqualTo("BELGIQUE");
    }

    @Test
    void baseJuridique_BE_motif_IST_avec_enfants_mentionne_387bis_Cciv() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ENLEVEMENT_INTERNATIONAL, true, true, true, true, "BELGIQUE");
        assertThat(r.baseJuridique()).contains("387bis");
    }

    // =============== Validation ===============

    @Test
    void motifNull_throws() {
        assertThatThrownBy(() -> OrdonnanceRequeteCalculator.compute(
                null, true, true, true, false, "FRANCE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Motif");
    }

    @Test
    void countryNull_throws() {
        assertThatThrownBy(() -> OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Pays");
    }

    @Test
    void countryNonSupporte_throws() {
        assertThatThrownBy(() -> OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, false, "ESPAGNE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("FRANCE et BELGIQUE");
    }

    @Test
    void countryBlank_throws() {
        assertThatThrownBy(() -> OrdonnanceRequeteCalculator.compute(
                MotifRequete.DETOURNEMENT_PATRIMOINE, true, true, true, false, "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void formule_lisible_contient_motif_score_verdict_delai() {
        OrdonnanceRequeteResult r = OrdonnanceRequeteCalculator.compute(
                MotifRequete.ENLEVEMENT_INTERNATIONAL, true, true, true, true, "FRANCE");
        assertThat(r.formule())
                .contains("ENLEVEMENT_INTERNATIONAL")
                .contains("ELEVEE")
                .contains("100")
                .contains("1-3");
    }
}
