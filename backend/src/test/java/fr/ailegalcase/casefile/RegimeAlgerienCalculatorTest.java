package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegimeAlgerienCalculatorTest {

    // ---- CRA 1 AN (art. 5) -----------------------------------------------

    @Test
    void cra1An_visaLongSejour_ELEVEE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_1_AN", true, 0, true, true,
                null, null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.voieDemande()).isEqualTo("CRA_1_AN");
        assertThat(r.dureeTitreAnnees()).isEqualTo(1);
        assertThat(r.baseJuridique()).contains("27/12/1968").contains("art. 5");
        assertThat(r.delaiInstructionMois()).isEqualTo(3);
        assertThat(r.documentsRequis()).isNotEmpty();
    }

    @Test
    void cra1An_sansVisaLongSejour_FAIBLE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_1_AN", true, 0, true, false,
                null, null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Visa de long séjour"));
    }

    @Test
    void cra1An_casierNonVierge_forcesFAIBLE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_1_AN", true, 0, false, true,
                null, null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Casier"));
    }

    @Test
    void cra1An_etatCivilManquant_forcesFAIBLE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_1_AN", false, 0, true, true,
                null, null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("état civil"));
    }

    // ---- CRA 10 ANS LIEN FRANCE (art. 6) ----------------------------------

    @Test
    void cra10AnsLienFrance_conjointFr_ELEVEE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_LIEN_FRANCE", true, 24, true, null,
                true, false, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.dureeTitreAnnees()).isEqualTo(10);
        assertThat(r.baseJuridique()).contains("art. 6");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("conjoint français"));
    }

    @Test
    void cra10AnsLienFrance_parentEnfantFr_ELEVEE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_LIEN_FRANCE", true, 24, true, null,
                false, true, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("parent d'enfant français"));
    }

    @Test
    void cra10AnsLienFrance_10ansPresence_ELEVEE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_LIEN_FRANCE", true, 120, true, null,
                false, false, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("10 ans de présence"));
    }

    @Test
    void cra10AnsLienFrance_aucunLien_FAIBLE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_LIEN_FRANCE", true, 36, true, null,
                false, false, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Aucun lien"));
    }

    // ---- CRA 10 ANS RÉSIDENT ANCIEN (art. 7bis) ---------------------------

    @Test
    void cra10AnsResidentAncien_neEnFrance_ELEVEE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_RESIDENT_ANCIEN", true, null, true, null,
                null, null, true, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("art. 7bis");
    }

    @Test
    void cra10AnsResidentAncien_arriveAvant13_ELEVEE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_RESIDENT_ANCIEN", true, null, true, null,
                null, null, false, true, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void cra10AnsResidentAncien_arriveApres13_FAIBLE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_RESIDENT_ANCIEN", true, null, true, null,
                null, null, false, false, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("art. 7bis"));
    }

    // ---- CHANGEMENT VERS TRAVAILLEUR (art. 7) -----------------------------

    @Test
    void changementTravailleur_avecContrat_MOYENNE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CHANGEMENT_VERS_TRAVAILLEUR", true, 12, true, null,
                null, null, null, null, true, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.baseJuridique()).contains("art. 7");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("discrétionnaire"));
    }

    @Test
    void changementTravailleur_sansContrat_FAIBLE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CHANGEMENT_VERS_TRAVAILLEUR", true, 12, true, null,
                null, null, null, null, false, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Contrat"));
    }

    // ---- REGROUPEMENT FAMILIAL (art. 4) -----------------------------------

    @Test
    void regroupement_ressourcesOk_logementOk_ELEVEE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "REGROUPEMENT_FAMILIAL_ACCORD_1968", true, 36, true, null,
                null, null, null, null, null, true, true, 4);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("art. 4");
        assertThat(r.delaiInstructionMois()).isEqualTo(6);
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Composition du foyer"));
    }

    @Test
    void regroupement_logementInsuffisant_MOYENNE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "REGROUPEMENT_FAMILIAL_ACCORD_1968", true, 36, true, null,
                null, null, null, null, null, true, false, 4);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Logement"));
    }

    @Test
    void regroupement_ressourcesInsuffisantes_FAIBLE() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "REGROUPEMENT_FAMILIAL_ACCORD_1968", true, 36, true, null,
                null, null, null, null, null, false, true, 4);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Ressources"));
    }

    // ---- Validations / erreurs --------------------------------------------

    @Test
    void voieInconnue_throws() {
        assertThatThrownBy(() -> RegimeAlgerienCalculator.compute(
                "INCONNU", true, 0, true, true,
                null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voie non supportée");
    }

    @Test
    void voieNull_throws() {
        assertThatThrownBy(() -> RegimeAlgerienCalculator.compute(
                null, true, 0, true, true,
                null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voieDemande");
    }

    @Test
    void voieBlank_throws() {
        assertThatThrownBy(() -> RegimeAlgerienCalculator.compute(
                "   ", true, 0, true, true,
                null, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voieDemande");
    }

    @Test
    void presenceFranceNegative_throws() {
        assertThatThrownBy(() -> RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_LIEN_FRANCE", true, -1, true, null,
                true, null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("presenceReguliereFranceMois");
    }

    @Test
    void nbPersonnesFoyerNegative_throws() {
        assertThatThrownBy(() -> RegimeAlgerienCalculator.compute(
                "REGROUPEMENT_FAMILIAL_ACCORD_1968", true, 36, true, null,
                null, null, null, null, null, true, true, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nombrePersonnesFoyer");
    }

    // ---- Méta -------------------------------------------------------------

    @Test
    void caseInsensitive_voie() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "cra_1_an", true, 0, true, true,
                null, null, null, null, null, null, null, null);

        assertThat(r.voieDemande()).isEqualTo("CRA_1_AN");
        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void formule_containsVoieAndVerdict() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_1_AN", true, 0, true, true,
                null, null, null, null, null, null, null, null);

        assertThat(r.formule()).contains("Régime algérien").contains("ELEVEE");
    }

    @Test
    void delais_perVoieAreCorrect() {
        RegimeAlgerienResult cra1 = RegimeAlgerienCalculator.compute(
                "CRA_1_AN", true, 0, true, true,
                null, null, null, null, null, null, null, null);
        RegimeAlgerienResult cra10 = RegimeAlgerienCalculator.compute(
                "CRA_10_ANS_LIEN_FRANCE", true, 24, true, null,
                true, null, null, null, null, null, null, null);
        RegimeAlgerienResult regroup = RegimeAlgerienCalculator.compute(
                "REGROUPEMENT_FAMILIAL_ACCORD_1968", true, 36, true, null,
                null, null, null, null, null, true, true, 3);

        assertThat(cra1.delaiInstructionMois()).isEqualTo(3);
        assertThat(cra10.delaiInstructionMois()).isEqualTo(3);
        assertThat(regroup.delaiInstructionMois()).isEqualTo(6);
    }

    @Test
    void messages_includesAccordReference() {
        RegimeAlgerienResult r = RegimeAlgerienCalculator.compute(
                "CRA_1_AN", true, 0, true, true,
                null, null, null, null, null, null, null, null);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("ressortissants algériens"));
    }
}
