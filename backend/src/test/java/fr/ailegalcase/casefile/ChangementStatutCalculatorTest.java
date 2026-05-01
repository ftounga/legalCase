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

    // ---- SF-IM-11-04 : transitions vers VPF -------------------------------

    @Test
    void etudiantVersVpf_avecJustificatif_verdictEleve() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "VPF",
                4, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("L.423-1");
        assertThat(r.documentsRequis()).anySatisfy(d ->
                assertThat(d).contains("Acte civil de référence"));
        assertThat(r.documentsRequis()).anySatisfy(d ->
                assertThat(d).contains("Justificatif de domicile commun"));
        assertThat(r.formule()).contains("ETUDIANT").contains("VPF");
    }

    @Test
    void etudiantVersVpf_sansJustificatif_verdictFaible() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "VPF",
                4, false, null, true);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Acte civil de référence")
                        .contains("non produit"));
    }

    @Test
    void visiteurVersVpf_verdictEleve() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "VISITEUR", "VPF",
                6, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("L.423-1");
    }

    @Test
    void salarieVersVpf_verdictEleve() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "SALARIE", "VPF",
                6, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("L.423-1");
    }

    @Test
    void passeportTalentVersVpf_verdictEleve() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "PASSEPORT_TALENT_SALARIE_QUALIFIE", "VPF",
                8, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("L.423-1");
    }

    @Test
    void apsVersVpf_delai2Mois() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "APS", "VPF",
                6, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        // APS spécifique préservé : délai d'instruction 2 mois (APS source ou destination).
        assertThat(r.delaiInstructionMois()).isEqualTo(2);
    }

    @Test
    void vpfDureeBloque_dureeRestante1Mois_retrogradeFaible() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "VPF",
                1, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Durée restante"));
    }

    @Test
    void vpfCasierNonVierge_retrogradeFaible() {
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "VPF",
                4, true, null, false);

        assertThat(r.verdictTransition()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Casier judiciaire"));
    }

    @Test
    void vpfRemunerationNonExigee() {
        // La rémunération n'est PAS exigée pour les transitions vers VPF
        // (à la différence des transitions vers SALARIE qui imposent SMIC × 1,5).
        ChangementStatutResult r = ChangementStatutCalculator.compute(
                "ETUDIANT", "VPF",
                4, true, null, true);

        assertThat(r.verdictTransition()).isEqualTo("ELEVEE");
        assertThat(r.risqueRefus())
                .noneSatisfy(m -> assertThat(m).contains("SMIC"));
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
        // SF-IM-11-04 : SALARIE → VPF est désormais supportée. On utilise une paire
        // explicitement non couverte (ASILE → VPF) pour valider la branche d'erreur.
        assertThatThrownBy(() -> ChangementStatutCalculator.compute(
                "ASILE", "VPF", 10, true, null, true))
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
