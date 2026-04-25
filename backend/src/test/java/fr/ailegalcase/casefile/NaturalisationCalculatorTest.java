package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NaturalisationCalculatorTest {

    // ---- DECRET (art. 21-15) -----------------------------------------------

    @Test
    void decret_5ans_langueOk_ressourcesOk_returnsELEVEE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 5, null, null, null, null, null, null, null,
                true, true, true, false, false);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.voieNaturalisation()).isEqualTo("DECRET");
        assertThat(r.baseJuridique()).contains("21-15");
        assertThat(r.delaiInstructionMois()).isEqualTo(18);
        assertThat(r.documentsAFournir()).isNotEmpty();
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("discrétionnaire"));
    }

    @Test
    void decret_2ans_etudesSuperieuresFrance_returnsELEVEE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 2, null, null, null, null, null, null, null,
                true, true, true, false, true);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("21-18"));
    }

    @Test
    void decret_4ans_sansEtudes_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 4, null, null, null, null, null, null, null,
                true, true, true, false, false);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Durée de résidence"));
    }

    @Test
    void decret_5ans_sansLangueB1_returnsMOYENNE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 5, null, null, null, null, null, null, null,
                true, false, true, false, false);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("linguistique"));
    }

    @Test
    void decret_5ans_sansRessources_returnsMOYENNE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 5, null, null, null, null, null, null, null,
                true, true, false, false, false);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Ressources"));
    }

    // ---- MARIAGE (art. 21-2) -----------------------------------------------

    @Test
    void mariage_4ans_cohabitation_langueOk_returnsELEVEE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MARIAGE", null, 4, true, null, null, null, null, null,
                true, true, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("21-2");
        assertThat(r.delaiInstructionMois()).isEqualTo(12);
    }

    @Test
    void mariage_4ans_sansCohabitation_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MARIAGE", null, 4, false, null, null, null, null, null,
                true, true, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Durée de mariage"));
    }

    @Test
    void mariage_5ans_sansCohabitation_langueOk_returnsELEVEE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MARIAGE", null, 5, false, null, null, null, null, null,
                true, true, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void mariage_3ans_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MARIAGE", null, 3, true, null, null, null, null, null,
                true, true, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
    }

    @Test
    void mariage_4ans_cohabitation_sansLangue_returnsMOYENNE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MARIAGE", null, 4, true, null, null, null, null, null,
                true, false, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
    }

    // ---- ASCENDANT (art. 21-13-1) ------------------------------------------

    @Test
    void ascendant_65_25ans_directFrancais_returnsELEVEE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "ASCENDANT", 25, null, null, 65, true, null, null, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("21-13-1");
    }

    @Test
    void ascendant_64_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "ASCENDANT", 25, null, null, 64, true, null, null, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Âge"));
    }

    @Test
    void ascendant_65_20ansResidence_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "ASCENDANT", 20, null, null, 65, true, null, null, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("résidence"));
    }

    @Test
    void ascendant_65_25ans_sansLien_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "ASCENDANT", 25, null, null, 65, false, null, null, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("ascendant direct"));
    }

    // ---- MINEUR (art. 22-1) -------------------------------------------------

    @Test
    void mineur_parentAcquiert_vitAvec_returnsELEVEE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MINEUR", null, null, null, null, null, true, true, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("22-1");
        assertThat(r.delaiInstructionMois()).isEqualTo(6);
    }

    @Test
    void mineur_parentAcquiert_neVitPas_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MINEUR", null, null, null, null, null, true, false, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("habituellement"));
    }

    @Test
    void mineur_sansParent_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "MINEUR", null, null, null, null, null, false, true, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Aucun parent"));
    }

    // ---- REINTEGRATION (art. 24-1+) -----------------------------------------

    @Test
    void reintegration_ancienFrancais_returnsELEVEE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "REINTEGRATION", null, null, null, null, null, null, null, true,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("24-1");
    }

    @Test
    void reintegration_jamaisFrancais_returnsFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "REINTEGRATION", null, null, null, null, null, null, null, false,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("jamais été français"));
    }

    // ---- OPPOSITION ---------------------------------------------------------

    @Test
    void opposition_returnsMOYENNE_withRecoursMessage() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "OPPOSITION", null, null, null, null, null, null, null, null,
                true, null, null, false, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.baseJuridique()).contains("21-4").contains("27-2");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Conseil d'État"));
    }

    // ---- Bloques transversaux ---------------------------------------------

    @Test
    void oppositionGouvernementaleActive_forcesFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 5, null, null, null, null, null, null, null,
                true, true, true, true, false);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Opposition gouvernementale active"));
    }

    @Test
    void casierNonVierge_forcesFAIBLE() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 5, null, null, null, null, null, null, null,
                false, true, true, false, false);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(c ->
                assertThat(c).contains("Casier judiciaire"));
    }

    // ---- Validations / erreurs --------------------------------------------

    @Test
    void voieInconnue_throws() {
        assertThatThrownBy(() -> NaturalisationCalculator.compute(
                "INCONNU", 5, null, null, null, null, null, null, null,
                true, true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Voie non supportée");
    }

    @Test
    void voieNull_throws() {
        assertThatThrownBy(() -> NaturalisationCalculator.compute(
                null, 5, null, null, null, null, null, null, null,
                true, true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voieNaturalisation");
    }

    @Test
    void voieBlank_throws() {
        assertThatThrownBy(() -> NaturalisationCalculator.compute(
                "   ", 5, null, null, null, null, null, null, null,
                true, true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("voieNaturalisation");
    }

    @Test
    void dureeResidenceNegative_throws() {
        assertThatThrownBy(() -> NaturalisationCalculator.compute(
                "DECRET", -1, null, null, null, null, null, null, null,
                true, true, true, false, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeResidence");
    }

    @Test
    void dureeMariageNegative_throws() {
        assertThatThrownBy(() -> NaturalisationCalculator.compute(
                "MARIAGE", null, -1, true, null, null, null, null, null,
                true, true, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeMariage");
    }

    @Test
    void ageNegative_throws() {
        assertThatThrownBy(() -> NaturalisationCalculator.compute(
                "ASCENDANT", 25, null, null, -1, true, null, null, null,
                true, null, null, false, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageDemandeur");
    }

    // ---- Méta -------------------------------------------------------------

    @Test
    void caseInsensitive_voie() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "decret", 5, null, null, null, null, null, null, null,
                true, true, true, false, false);

        assertThat(r.voieNaturalisation()).isEqualTo("DECRET");
        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void formule_containsVoieAndVerdict() {
        NaturalisationResult r = NaturalisationCalculator.compute(
                "DECRET", 5, null, null, null, null, null, null, null,
                true, true, true, false, false);

        assertThat(r.formule()).contains("Naturalisation").contains("ELEVEE");
    }

    @Test
    void delais_perVoieAreCorrect() {
        NaturalisationResult decret = NaturalisationCalculator.compute(
                "DECRET", 5, null, null, null, null, null, null, null,
                true, true, true, false, false);
        NaturalisationResult mariage = NaturalisationCalculator.compute(
                "MARIAGE", null, 4, true, null, null, null, null, null,
                true, true, null, false, null);
        NaturalisationResult mineur = NaturalisationCalculator.compute(
                "MINEUR", null, null, null, null, null, true, true, null,
                true, null, null, false, null);

        assertThat(decret.delaiInstructionMois()).isEqualTo(18);
        assertThat(mariage.delaiInstructionMois()).isEqualTo(12);
        assertThat(mineur.delaiInstructionMois()).isEqualTo(6);
    }
}
