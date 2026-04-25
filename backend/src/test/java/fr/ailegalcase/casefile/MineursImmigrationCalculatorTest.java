package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MineursImmigrationCalculatorTest {

    private static final LocalDate ANALYSE = LocalDate.of(2026, 4, 25);

    // ---- MNA_ORDONNANCE_JE -----------------------------------------------

    @Test
    void mna_mineur15ans_isolementAvere_returnsELEVEE() {
        LocalDate naissance = ANALYSE.minusYears(15);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "MNA_ORDONNANCE_JE", naissance, null,
                false, true, false, "Côte d'Ivoire", ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
        assertThat(r.dispositifRecommande()).isEqualTo("MNA_ORDONNANCE_JE");
        assertThat(r.ageAnnees()).isEqualTo(15);
        assertThat(r.baseJuridique()).contains("Cciv art. 375").contains("L.221-2-2");
        assertThat(r.delaiInstructionMois()).isEqualTo(4);
    }

    @Test
    void mna_majeur18ans_returnsFAIBLE() {
        LocalDate naissance = ANALYSE.minusYears(18);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "MNA_ORDONNANCE_JE", naissance, null,
                false, true, false, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(m ->
                assertThat(m).contains("Majorité atteinte"));
    }

    @Test
    void mna_mineurSansIsolement_returnsFAIBLE() {
        LocalDate naissance = ANALYSE.minusYears(15);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "MNA_ORDONNANCE_JE", naissance, null,
                false, false, false, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(m ->
                assertThat(m).contains("Isolement non avéré"));
    }

    @Test
    void mna_mineur17ansLimite_returnsMOYENNE() {
        // 17 ans 6 mois → minorité contestable
        LocalDate naissance = ANALYSE.minusYears(17).minusMonths(6);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "MNA_ORDONNANCE_JE", naissance, null,
                false, true, false, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("MOYENNE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("contestée"));
    }

    // ---- TITRE_SEJOUR_L435_3 ---------------------------------------------

    @Test
    void l435_3_neEnFrance_3ans_parentRegulier_returnsELEVEE() {
        LocalDate naissance = ANALYSE.minusYears(5);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "TITRE_SEJOUR_L435_3", naissance, naissance,
                true, false, false, "Mali", ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("L.435-3");
        assertThat(r.delaiInstructionMois()).isEqualTo(6);
    }

    @Test
    void l435_3_residenceMoins3ans_returnsFAIBLE() {
        LocalDate naissance = ANALYSE.minusYears(2);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "TITRE_SEJOUR_L435_3", naissance, naissance,
                true, false, false, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(m ->
                assertThat(m).contains("Résidence en France"));
    }

    @Test
    void l435_3_parentNonRegulier_returnsFAIBLE() {
        LocalDate naissance = ANALYSE.minusYears(5);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "TITRE_SEJOUR_L435_3", naissance, naissance,
                false, false, false, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(m ->
                assertThat(m).contains("Aucun parent en situation régulière"));
    }

    @Test
    void l435_3_nonNeEnFrance_returnsFAIBLE() {
        LocalDate naissance = ANALYSE.minusYears(10);
        // entrée en France 5 ans après naissance → pas né en France
        LocalDate entree = naissance.plusYears(5);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "TITRE_SEJOUR_L435_3", naissance, entree,
                true, false, false, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(m ->
                assertThat(m).contains("non né en France"));
    }

    // ---- DCEM -------------------------------------------------------------

    @Test
    void dcem_mineurSansOrdrePublic_returnsELEVEE() {
        LocalDate naissance = ANALYSE.minusYears(10);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "DCEM", naissance, null,
                false, false, false, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("R.321-3");
        assertThat(r.delaiInstructionMois()).isEqualTo(2);
    }

    @Test
    void dcem_motifOrdrePublic_returnsFAIBLE() {
        LocalDate naissance = ANALYSE.minusYears(10);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "DCEM", naissance, null,
                false, false, true, null, ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("FAIBLE");
        assertThat(r.criteresNonRemplis()).anySatisfy(m ->
                assertThat(m).contains("Motif d'ordre public"));
    }

    // ---- TIR --------------------------------------------------------------

    @Test
    void tir_mineurApatride_returnsELEVEE() {
        LocalDate naissance = ANALYSE.minusYears(8);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "TIR", naissance, null,
                false, false, false, "APATRIDE", ANALYSE);

        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
        assertThat(r.baseJuridique()).contains("R.321-7");
        assertThat(r.delaiInstructionMois()).isEqualTo(3);
    }

    // ---- Bloque transversal ----------------------------------------------

    @Test
    void majeur_forcesFAIBLE_surTousLesDispositifs() {
        LocalDate naissance = ANALYSE.minusYears(20);
        for (String d : new String[]{"MNA_ORDONNANCE_JE", "TITRE_SEJOUR_L435_3", "DCEM", "TIR"}) {
            MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                    d, naissance, naissance,
                    true, true, false, null, ANALYSE);
            assertThat(r.verdictEligibilite()).as("dispositif=%s", d).isEqualTo("FAIBLE");
            assertThat(r.criteresNonRemplis()).anySatisfy(m ->
                    assertThat(m).contains("Majorité atteinte"));
        }
    }

    // ---- Validations / erreurs ------------------------------------------

    @Test
    void dispositifNonSupporte_throws() {
        LocalDate naissance = ANALYSE.minusYears(10);
        assertThatThrownBy(() -> MineursImmigrationCalculator.compute(
                "AUTRE", naissance, null, false, false, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dispositif non supporté");
    }

    @Test
    void dispositifNull_throws() {
        LocalDate naissance = ANALYSE.minusYears(10);
        assertThatThrownBy(() -> MineursImmigrationCalculator.compute(
                null, naissance, null, false, false, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositifVise");
    }

    @Test
    void dateNaissanceNull_throws() {
        assertThatThrownBy(() -> MineursImmigrationCalculator.compute(
                "MNA_ORDONNANCE_JE", null, null, false, false, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateNaissance");
    }

    @Test
    void dateNaissanceFuture_throws() {
        LocalDate naissance = ANALYSE.plusDays(1);
        assertThatThrownBy(() -> MineursImmigrationCalculator.compute(
                "MNA_ORDONNANCE_JE", naissance, null, false, false, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("futur");
    }

    @Test
    void dateEntreeAvantNaissance_throws() {
        LocalDate naissance = ANALYSE.minusYears(5);
        LocalDate entree = naissance.minusYears(1);
        assertThatThrownBy(() -> MineursImmigrationCalculator.compute(
                "TITRE_SEJOUR_L435_3", naissance, entree,
                true, false, false, null, ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateEntreeFrance");
    }

    // ---- Méta ------------------------------------------------------------

    @Test
    void delaisCoherentsParDispositif() {
        LocalDate naissance = ANALYSE.minusYears(10);

        MineursImmigrationResult mna = MineursImmigrationCalculator.compute(
                "MNA_ORDONNANCE_JE", naissance, null, false, true, false, null, ANALYSE);
        MineursImmigrationResult l435 = MineursImmigrationCalculator.compute(
                "TITRE_SEJOUR_L435_3", naissance, naissance, true, false, false, null, ANALYSE);
        MineursImmigrationResult dcem = MineursImmigrationCalculator.compute(
                "DCEM", naissance, null, false, false, false, null, ANALYSE);
        MineursImmigrationResult tir = MineursImmigrationCalculator.compute(
                "TIR", naissance, null, false, false, false, null, ANALYSE);

        assertThat(mna.delaiInstructionMois()).isEqualTo(4);
        assertThat(l435.delaiInstructionMois()).isEqualTo(6);
        assertThat(dcem.delaiInstructionMois()).isEqualTo(2);
        assertThat(tir.delaiInstructionMois()).isEqualTo(3);
    }

    @Test
    void documentsRequis_remplis_pourChaqueDispositif() {
        LocalDate naissance = ANALYSE.minusYears(10);

        for (String d : new String[]{"MNA_ORDONNANCE_JE", "TITRE_SEJOUR_L435_3", "DCEM", "TIR"}) {
            MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                    d, naissance, naissance, true, true, false, null, ANALYSE);
            assertThat(r.documentsRequis()).as("dispositif=%s", d).isNotEmpty();
        }
    }

    @Test
    void caseInsensitive_dispositif() {
        LocalDate naissance = ANALYSE.minusYears(10);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "dcem", naissance, null, false, false, false, null, ANALYSE);
        assertThat(r.dispositifRecommande()).isEqualTo("DCEM");
        assertThat(r.verdictEligibilite()).isEqualTo("ELEVEE");
    }

    @Test
    void formuleContientDispositifEtAge() {
        LocalDate naissance = ANALYSE.minusYears(12);
        MineursImmigrationResult r = MineursImmigrationCalculator.compute(
                "DCEM", naissance, null, false, false, false, null, ANALYSE);
        assertThat(r.formule()).contains("DCEM").contains("12");
    }
}
