package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChangementStatutCalculatorTest {

    private static final BigDecimal SMIC = ChangementStatutCalculator.SMIC_MENSUEL_BRUT_EUR_2026; // 1801.80
    private static final BigDecimal SMIC_1_5 = SMIC.multiply(new BigDecimal("1.5")); // 2702.70
    private static final BigDecimal SMIC_1_2 = SMIC.multiply(new BigDecimal("1.2")); // 2162.16

    // ---- ETUDIANT → SALARIE -----------------------------------------------

    @Test
    void etudiantToSalarie_remuOk_justificatifOk_returnsELEVEE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                10, true, SMIC_1_5, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.documentsRequis()).isNotEmpty();
        assertThat(r.baseJuridique()).contains("L.421-1").contains("R.5221-3");
        assertThat(r.formule()).contains("ETUDIANT").contains("SALARIE");
    }

    @Test
    void etudiantToSalarie_remuEntreSmicEt1_5_returnsMOYENNE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                10, true, SMIC_1_2, true);

        assertThat(r.verdictTransition()).isEqualTo("MOYENNE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("1,5 SMIC"));
    }

    @Test
    void etudiantToSalarie_remuSousSmic_returnsFAIBLE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                10, true, new BigDecimal("1500.00"), true);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("inférieure au SMIC"));
    }

    @Test
    void etudiantToSalarie_sansJustificatif_returnsFAIBLE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                10, false, SMIC_1_5, true);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Contrat de travail non fourni"));
    }

    @Test
    void etudiantToSalarie_remuNull_returnsMOYENNE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                10, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("MOYENNE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("non communiquée"));
    }

    // ---- ETUDIANT → APS ---------------------------------------------------

    @Test
    void etudiantToAps_avecJustificatif_returnsELEVEE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "APS",
                10, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.delaiInstructionMois()).isEqualTo(2);
        assertThat(r.baseJuridique()).contains("L.422-10");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("APS art. L.422-10"));
    }

    @Test
    void etudiantToAps_sansJustificatif_returnsFAIBLE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "APS",
                10, false, null, true);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
    }

    // ---- VISITEUR → SALARIE ------------------------------------------------

    @Test
    void visiteurToSalarie_avecContrat_returnsMOYENNE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "VISITEUR", "SALARIE",
                10, true, SMIC_1_5, true);

        assertThat(r.verdictTransition()).isEqualTo("MOYENNE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("DREETS"));
    }

    @Test
    void visiteurToSalarie_sansContrat_returnsFAIBLE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "VISITEUR", "SALARIE",
                10, false, SMIC_1_5, true);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
    }

    // ---- VPF → SALARIE / ETUDIANT -----------------------------------------

    @Test
    void vpfToSalarie_returnsELEVEE_withSimplifiedMessage() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "VPF", "SALARIE",
                10, true, SMIC_1_5, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("autorise déjà"));
    }

    @Test
    void vpfToEtudiant_avecJustificatif_returnsELEVEE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "VPF", "ETUDIANT",
                10, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("L.422-1");
    }

    // ---- TALENT_INTRA / TALENT_SALARIE ------------------------------------

    @Test
    void talentSalarieQualifieToInnovant_returnsMOYENNE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "PASSEPORT_TALENT_SALARIE_QUALIFIE", "PASSEPORT_TALENT_INNOVANT",
                10, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("MOYENNE");
        assertThat(r.baseJuridique()).contains("L.421-9").contains("L.421-15");
    }

    @Test
    void talentToSalarieClassique_returnsMOYENNE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "PASSEPORT_TALENT_SALARIE_QUALIFIE", "SALARIE",
                10, true, SMIC_1_5, true);

        assertThat(r.verdictTransition()).isEqualTo("MOYENNE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("Sortie du dispositif"));
    }

    // ---- Bloques transversaux --------------------------------------------

    @Test
    void dureeRestanteSousSeuil_forcesFAIBLE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                1, true, SMIC_1_5, true);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Durée restante"));
    }

    @Test
    void casierNonVierge_forcesFAIBLE() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                10, true, SMIC_1_5, false);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Casier judiciaire"));
    }

    // ---- Validations / erreurs --------------------------------------------

    @Test
    void transitionIdentique_throws() {
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                "ETUDIANT", "ETUDIANT", 10, true, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("identiques");
    }

    @Test
    void transitionNonSupportee_throws() {
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                "SALARIE", "VPF", 10, true, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non supportée");
    }

    @Test
    void titreActuelNull_throws() {
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                null, "SALARIE", 10, true, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titreActuel");
    }

    @Test
    void titreEnvisageNull_throws() {
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                "ETUDIANT", null, 10, true, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titreEnvisage");
    }

    @Test
    void titreActuelBlank_throws() {
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                "   ", "SALARIE", 10, true, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("titreActuel");
    }

    @Test
    void dureeRestanteNegative_throws() {
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE", -1, true, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dureeRestante");
    }

    @Test
    void remunerationNegative_throws() {
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE", 10, true, new BigDecimal("-100"), true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("remuneration");
    }

    // ---- Méta -------------------------------------------------------------

    @Test
    void delaiApsEst2_delaiDefaultEst3() {
        ChangementStatutResult aps = ChangementStatutCalculator.compute(
                "ETUDIANT", "APS", 10, true, null, true);
        ChangementStatutResult sal = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE", 10, true, SMIC_1_5, true);

        assertThat(aps.delaiInstructionMois()).isEqualTo(2);
        assertThat(sal.delaiInstructionMois()).isEqualTo(3);
    }

    @Test
    void messages_containDeposeAvantExpiration() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "SALARIE",
                10, true, SMIC_1_5, true);

        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("avant l'expiration"));
    }

    @Test
    void caseInsensitive_titres() {
        // les titres sont normalisés en MAJUSCULES
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "etudiant", "salarie",
                10, true, SMIC_1_5, true);

        assertThat(r.titreActuel()).isEqualTo("ETUDIANT");
        assertThat(r.titreEnvisage()).isEqualTo("SALARIE");
        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
    }
}
