package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PmaGpaBioethiqueCalculatorTest {

    // ---- PMA — Reconnaissance anticipée (art. 342-9 et s. Cciv loi 2021) ---------

    @Test
    void pma_notaireOk_dateAnterieure_conditionsOk_returnsELEVEE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 4, 1), true,
                null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.risqueRefus()).isEqualTo("FAIBLE");
        assertThat(r.dispositif()).isEqualTo("PMA_RECONNAISSANCE_ANTICIPEE");
        assertThat(r.delaiInstructionMois()).isEqualTo(1);
        assertThat(r.baseJuridique()).contains("342-9");
        assertThat(r.baseJuridique()).contains("2021");
        assertThat(r.documentsRequis()).isNotEmpty();
        assertThat(r.proceduresAFollow()).isNotEmpty();
    }

    @Test
    void pma_sansNotaire_returnsFAIBLE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                false, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 4, 1), true,
                null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).isEqualTo("ELEVE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("notarié"));
    }

    @Test
    void pma_dateReconnaissancePosterieureALaPMA_returnsFAIBLE() {
        // Reconnaissance le 1/4/2024, PMA le 15/1/2024 → POSTÉRIEURE = invalide
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 1, 15), true,
                null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("précéder"));
    }

    @Test
    void pma_conditionsAccesNonRemplies_returnsFAIBLE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, LocalDate.of(2024, 1, 15), LocalDate.of(2024, 4, 1), false,
                null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("conditions d'accès"));
    }

    @Test
    void pma_delaiInstruction1Mois() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, null, null, true,
                null, null, null, null, null, null, null);

        assertThat(r.delaiInstructionMois()).isEqualTo(1);
    }

    @Test
    void pma_datesNonRenseignees_presomptionAnteriorite() {
        // Si dates non renseignées, on ne bloque pas pour antériorité
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, null, null, true,
                null, null, null, null, null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
    }

    // ---- GPA — Transcription état civil (Cass. 4 arrêts 18/12/2022) -------------

    @Test
    void gpa_paysLegal_biologique_decision_adoption_returnsELEVEE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                true, true, true, true,
                null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.risqueRefus()).isEqualTo("FAIBLE");
        assertThat(r.dispositif()).isEqualTo("GPA_TRANSCRIPTION_ETAT_CIVIL");
        assertThat(r.delaiInstructionMois()).isEqualTo(9);
        assertThat(r.baseJuridique()).contains("18/12/2022");
    }

    @Test
    void gpa_paysNonLegal_returnsFAIBLE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                false, true, true, true,
                null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).isEqualTo("ELEVE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("légale"));
    }

    @Test
    void gpa_biologiqueNonAvere_returnsFAIBLE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                true, false, true, true,
                null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("génétiquement"));
    }

    @Test
    void gpa_sansAdoption_returnsMOYENNE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                true, true, true, false,
                null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.risqueRefus()).isEqualTo("MOYEN");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("adoption"));
    }

    @Test
    void gpa_decisionEtrangereManquante_returnsFAIBLE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                true, true, false, true,
                null, null, null);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
    }

    @Test
    void gpa_delaiInstruction9Mois() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                true, true, true, true,
                null, null, null);

        assertThat(r.delaiInstructionMois()).isEqualTo(9);
    }

    // ---- Accès origines (art. 16-8-1 Cciv loi 2021) ------------------------------

    @Test
    void don_majeur_demande_donPosterieurEntreeVigueur_returnsELEVEE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null,
                null, null, null, null,
                LocalDate.of(2023, 5, 1), true, 22);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.risqueRefus()).isEqualTo("FAIBLE");
        assertThat(r.dispositif()).isEqualTo("DON_GAMETES_ACCES_ORIGINES");
        assertThat(r.delaiInstructionMois()).isEqualTo(6);
        assertThat(r.baseJuridique()).contains("16-8-1");
    }

    @Test
    void don_mineur_returnsFAIBLE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null,
                null, null, null, null,
                LocalDate.of(2023, 5, 1), true, 17);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("majeur"));
    }

    @Test
    void don_sansDemande_returnsFAIBLE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null,
                null, null, null, null,
                LocalDate.of(2023, 5, 1), false, 22);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
    }

    @Test
    void don_anterieurEntreeVigueur_returnsMOYENNE() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null,
                null, null, null, null,
                LocalDate.of(2010, 5, 1), true, 25);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.risqueRefus()).isEqualTo("MOYEN");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("transitoire"));
    }

    @Test
    void don_delaiInstruction6Mois() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null,
                null, null, null, null,
                null, true, 22);

        assertThat(r.delaiInstructionMois()).isEqualTo(6);
    }

    // ---- Validations / erreurs --------------------------------------------------

    @Test
    void dispositifInconnu_throws() {
        assertThatThrownBy(() -> PmaGpaBioethiqueCalculator.compute(
                "INCONNU", null, null, null, null,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dispositif non supporté");
    }

    @Test
    void dispositifNull_throws() {
        assertThatThrownBy(() -> PmaGpaBioethiqueCalculator.compute(
                null, null, null, null, null,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositif");
    }

    @Test
    void dispositifBlank_throws() {
        assertThatThrownBy(() -> PmaGpaBioethiqueCalculator.compute(
                "   ", null, null, null, null,
                null, null, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositif");
    }

    @Test
    void ageDemandeurNegatif_throws() {
        assertThatThrownBy(() -> PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null, null, null, null, null,
                null, true, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ageDemandeur");
    }

    // ---- Méta -------------------------------------------------------------

    @Test
    void caseInsensitive_dispositif() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "pma_reconnaissance_anticipee",
                true, null, null, true,
                null, null, null, null, null, null, null);

        assertThat(r.dispositif()).isEqualTo("PMA_RECONNAISSANCE_ANTICIPEE");
        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
    }

    @Test
    void formule_containsDispositifAndVerdict() {
        PmaGpaBioethiqueResult r = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, null, null, true,
                null, null, null, null, null, null, null);

        assertThat(r.formule()).contains("PMA").contains("ELEVEE");
    }

    @Test
    void delais_perDispositifAreCorrect() {
        PmaGpaBioethiqueResult pma = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, null, null, true,
                null, null, null, null, null, null, null);
        PmaGpaBioethiqueResult gpa = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                true, true, true, true,
                null, null, null);
        PmaGpaBioethiqueResult don = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null,
                null, null, null, null,
                null, true, 22);

        assertThat(pma.delaiInstructionMois()).isEqualTo(1);
        assertThat(gpa.delaiInstructionMois()).isEqualTo(9);
        assertThat(don.delaiInstructionMois()).isEqualTo(6);
    }

    @Test
    void baseJuridique_containsExpectedRefs() {
        PmaGpaBioethiqueResult pma = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, null, null, true,
                null, null, null, null, null, null, null);
        PmaGpaBioethiqueResult gpa = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null,
                true, true, true, true,
                null, null, null);
        PmaGpaBioethiqueResult don = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null,
                null, null, null, null,
                null, true, 22);

        assertThat(pma.baseJuridique()).contains("342-9");
        assertThat(gpa.baseJuridique()).contains("18/12/2022");
        assertThat(don.baseJuridique()).contains("16-8-1");
    }

    @Test
    void proceduresAFollow_neverEmpty() {
        PmaGpaBioethiqueResult pma = PmaGpaBioethiqueCalculator.compute(
                "PMA_RECONNAISSANCE_ANTICIPEE",
                true, null, null, true, null, null, null, null, null, null, null);
        PmaGpaBioethiqueResult gpa = PmaGpaBioethiqueCalculator.compute(
                "GPA_TRANSCRIPTION_ETAT_CIVIL",
                null, null, null, null, true, true, true, true, null, null, null);
        PmaGpaBioethiqueResult don = PmaGpaBioethiqueCalculator.compute(
                "DON_GAMETES_ACCES_ORIGINES",
                null, null, null, null, null, null, null, null, null, true, 22);

        assertThat(pma.proceduresAFollow()).isNotEmpty();
        assertThat(gpa.proceduresAFollow()).isNotEmpty();
        assertThat(don.proceduresAFollow()).isNotEmpty();
    }
}
