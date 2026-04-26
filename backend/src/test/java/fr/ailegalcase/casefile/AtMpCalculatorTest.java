package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtMpCalculatorTest {

    private static final LocalDate ANALYSE = LocalDate.of(2026, 4, 25);

    // ---- RECONNAISSANCE_AT --------------------------------------------------

    @Test
    void at_lieuTravail_etCmi_returnsELEVEE_90j_CPAM() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_AT",
                LocalDate.of(2026, 3, 15), true, true, true,
                null, null, null, null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.delaiInstructionJours()).isEqualTo(90);
        assertThat(r.competence()).isEqualTo("CPAM");
        assertThat(r.expertiseRequise()).isFalse();
        assertThat(r.baseJuridique()).contains("L.411-1");
        assertThat(r.documentsRequis()).isNotEmpty();
        assertThat(r.formule()).contains("Reconnaissance AT");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Présomption"));
    }

    @Test
    void at_lieuTravailSeul_returnsMOYENNE() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_AT",
                LocalDate.of(2026, 3, 15), true, true, false,
                null, null, null, null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Certificat médical initial absent"));
    }

    @Test
    void at_cmiSeul_returnsMOYENNE() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_AT",
                LocalDate.of(2026, 3, 15), false, true, true,
                null, null, null, null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Accident non survenu au temps et lieu du travail"));
    }

    @Test
    void at_niLieuNiCmi_returnsFAIBLE() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_AT",
                LocalDate.of(2026, 3, 15), false, true, false,
                null, null, null, null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Ni lieu de travail ni certificat"));
    }

    @Test
    void at_declarationTardive_addsWarning() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_AT",
                LocalDate.of(2026, 3, 15), true, false, true,
                null, null, null, null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.messages()).anySatisfy(m ->
                assertThat(m).contains("déclaration employeur tardive"));
    }

    // ---- RECONNAISSANCE_MP --------------------------------------------------

    @Test
    void mp_tableauDelaiCmi_returnsELEVEE_120j_CPAM() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_MP",
                null, null, null, true,
                "57", true, LocalDate.of(2025, 6, 1),
                null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.delaiInstructionJours()).isEqualTo(120);
        assertThat(r.competence()).isEqualTo("CPAM");
        assertThat(r.expertiseRequise()).isFalse();
        assertThat(r.baseJuridique()).contains("L.461-1");
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Tableau 57"));
    }

    @Test
    void mp_horsTableau_returnsMOYENNE_competenceCRRMP_expertiseRequise() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_MP",
                null, null, null, true,
                "HORS_TABLEAU", true, LocalDate.of(2025, 6, 1),
                null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.competence()).isEqualTo("CRRMP");
        assertThat(r.expertiseRequise()).isTrue();
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CRRMP"));
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Reconnaissance hors tableau"));
    }

    @Test
    void mp_delaiNonRespecte_returnsFAIBLE_CRRMP() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_MP",
                null, null, null, true,
                "30", false, LocalDate.of(2024, 1, 1),
                null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.competence()).isEqualTo("CRRMP");
        assertThat(r.expertiseRequise()).isTrue();
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Délai de prise en charge"));
    }

    @Test
    void mp_tableauSansCmi_returnsMOYENNE_CPAM() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_MP",
                null, null, null, false,
                "30bis", true, LocalDate.of(2025, 1, 1),
                null, null, null, null,
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.competence()).isEqualTo("CPAM");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Certificat médical initial manquant"));
    }

    // ---- CONTESTATION_TAUX_IPP ----------------------------------------------

    @Test
    void ipp_ecart17_avecExpertise_returnsELEVEE() {
        AtMpResult r = AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                8, 25, true, LocalDate.of(2026, 3, 1),
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("ELEVEE");
        assertThat(r.competence()).isEqualTo("CMRA");
        assertThat(r.expertiseRequise()).isTrue();
        assertThat(r.delaiInstructionJours()).isEqualTo(120);
        assertThat(r.baseJuridique()).contains("L.434-2");
    }

    @Test
    void ipp_ecart7_avecExpertise_returnsMOYENNE() {
        AtMpResult r = AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                10, 17, true, LocalDate.of(2026, 3, 1),
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("MOYENNE");
        assertThat(r.expertiseRequise()).isTrue();
    }

    @Test
    void ipp_ecart3_avecExpertise_returnsFAIBLE() {
        AtMpResult r = AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                10, 13, true, LocalDate.of(2026, 3, 1),
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m -> assertThat(m).contains("trop faible"));
    }

    @Test
    void ipp_sansExpertise_returnsFAIBLE() {
        AtMpResult r = AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                5, 30, false, LocalDate.of(2026, 3, 1),
                ANALYSE);

        assertThat(r.verdictRecevabilite()).isEqualTo("FAIBLE");
        assertThat(r.risqueRefus()).anySatisfy(m ->
                assertThat(m).contains("Sans rapport médical contradictoire"));
    }

    @Test
    void ipp_competenceCMRA_etDelai60_60() {
        AtMpResult r = AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                10, 25, true, LocalDate.of(2026, 3, 1),
                ANALYSE);

        assertThat(r.competence()).isEqualTo("CMRA");
        assertThat(r.delaiInstructionJours()).isEqualTo(120);
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("CMRA"));
        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains("Pôle Social"));
    }

    @Test
    void ipp_messageMentionnePremierAvisCpam() {
        LocalDate avis = LocalDate.of(2026, 3, 1);
        AtMpResult r = AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                10, 25, true, avis,
                ANALYSE);

        assertThat(r.messages()).anySatisfy(m -> assertThat(m).contains(avis.toString()));
    }

    // ---- Validation des entrées --------------------------------------------

    @Test
    void dispositifNull_throws() {
        assertThatThrownBy(() -> AtMpCalculator.compute(
                null, null, null, null, null, null, null, null, null, null, null, null,
                ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dispositif");
    }

    @Test
    void dispositifInconnu_throws() {
        assertThatThrownBy(() -> AtMpCalculator.compute(
                "AUTRE_DISP",
                null, null, null, null, null, null, null, null, null, null, null,
                ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Dispositif non supporté");
    }

    @Test
    void ipp_tauxFixeHorsBornes_throws() {
        assertThatThrownBy(() -> AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                150, 200, true, LocalDate.of(2026, 3, 1),
                ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxFixeParCpam");
    }

    @Test
    void ipp_tauxRevendiqueInferieurOuEgal_throws() {
        assertThatThrownBy(() -> AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                25, 25, true, LocalDate.of(2026, 3, 1),
                ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tauxRevendique");
    }

    @Test
    void at_dateAccidentFuture_throws() {
        assertThatThrownBy(() -> AtMpCalculator.compute(
                "RECONNAISSANCE_AT",
                LocalDate.of(2027, 1, 1), true, true, true,
                null, null, null, null, null, null, null,
                ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateAccident");
    }

    @Test
    void mp_dateExpositionFuture_throws() {
        assertThatThrownBy(() -> AtMpCalculator.compute(
                "RECONNAISSANCE_MP",
                null, null, null, true,
                "30", true, LocalDate.of(2027, 1, 1),
                null, null, null, null,
                ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dateExposition");
    }

    @Test
    void ipp_datePremierAvisCpamFuture_throws() {
        assertThatThrownBy(() -> AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                10, 25, true, LocalDate.of(2027, 3, 1),
                ANALYSE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("datePremierAvisCpam");
    }

    // ---- BaseJuridique cohérent --------------------------------------------

    @Test
    void baseJuridiqueAt_mentionneL411() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_AT",
                LocalDate.of(2026, 3, 15), true, true, true,
                null, null, null, null, null, null, null,
                ANALYSE);
        assertThat(r.baseJuridique()).contains("L.411-1");
    }

    @Test
    void baseJuridiqueMp_mentionneL461() {
        AtMpResult r = AtMpCalculator.compute(
                "RECONNAISSANCE_MP",
                null, null, null, true,
                "30", true, LocalDate.of(2025, 6, 1),
                null, null, null, null,
                ANALYSE);
        assertThat(r.baseJuridique()).contains("L.461-1");
    }

    @Test
    void baseJuridiqueIpp_mentionneL434() {
        AtMpResult r = AtMpCalculator.compute(
                "CONTESTATION_TAUX_IPP",
                null, null, null, null, null, null, null,
                10, 25, true, LocalDate.of(2026, 3, 1),
                ANALYSE);
        assertThat(r.baseJuridique()).contains("L.434-2");
    }
}
