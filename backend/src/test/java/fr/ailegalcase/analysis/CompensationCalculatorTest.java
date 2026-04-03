package fr.ailegalcase.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CompensationCalculatorTest {

    // ── Indemnité légale ──────────────────────────────────────────────────────

    @Test
    void licenciement_5ans_calculCorrect() {
        // 5 ans × 1/4 × 2400 = 3000
        Optional<CompensationCalculator.CompensationEstimate> result =
                CompensationCalculator.calculate("LICENCIEMENT", 5, 0, 2400.0);
        assertThat(result).isPresent();
        assertThat(result.get().indemnite()).isCloseTo(3000.0, within(0.01));
    }

    @Test
    void licenciement_6ans4mois_calculCorrect() {
        // (6 + 4/12) = 6.333 ans × 1/4 × 2800 = 4433.33
        Optional<CompensationCalculator.CompensationEstimate> result =
                CompensationCalculator.calculate("LICENCIEMENT", 6, 4, 2800.0);
        assertThat(result).isPresent();
        assertThat(result.get().indemnite()).isCloseTo(4433.33, within(1.0));
    }

    @Test
    void licenciement_plusDe10ans_formula1_4_puis_1_3() {
        // 12 ans : 10 × 1/4 + 2 × 1/3 = 2.5 + 0.666 = 3.166 mois × 3000 = 9500
        Optional<CompensationCalculator.CompensationEstimate> result =
                CompensationCalculator.calculate("LICENCIEMENT", 12, 0, 3000.0);
        assertThat(result).isPresent();
        assertThat(result.get().indemnite()).isCloseTo(9500.0, within(1.0));
    }

    @Test
    void licenciement_moinsDe1an_indemnitEgaleZero() {
        Optional<CompensationCalculator.CompensationEstimate> result =
                CompensationCalculator.calculate("LICENCIEMENT", 0, 8, 2000.0);
        assertThat(result).isPresent();
        assertThat(result.get().indemnite()).isEqualTo(0.0);
    }

    @Test
    void ruptureConventionnelle_memesRegles() {
        Optional<CompensationCalculator.CompensationEstimate> r1 =
                CompensationCalculator.calculate("LICENCIEMENT", 5, 0, 2400.0);
        Optional<CompensationCalculator.CompensationEstimate> r2 =
                CompensationCalculator.calculate("RUPTURE_CONVENTIONNELLE", 5, 0, 2400.0);
        assertThat(r1).isPresent();
        assertThat(r2).isPresent();
        assertThat(r1.get().indemnite()).isEqualTo(r2.get().indemnite());
    }

    @Test
    void typeNonReconnu_retourneEmpty() {
        assertThat(CompensationCalculator.calculate("DEMISSION", 5, 0, 2000.0)).isEmpty();
    }

    @Test
    void typeNull_retourneEmpty() {
        assertThat(CompensationCalculator.calculate(null, 5, 0, 2000.0)).isEmpty();
    }

    @Test
    void ancienneteEtSalaireNulls_retourneEmpty() {
        assertThat(CompensationCalculator.calculate("LICENCIEMENT", null, null, null)).isEmpty();
    }

    @Test
    void salaireMissing_donneesPartielles() {
        Optional<CompensationCalculator.CompensationEstimate> result =
                CompensationCalculator.calculate("LICENCIEMENT", 5, 0, null);
        assertThat(result).isPresent();
        assertThat(result.get().donneesPartielles()).isTrue();
        assertThat(result.get().indemnite()).isEqualTo(0.0); // no salary → 0
    }

    // ── Barème Macron ─────────────────────────────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "0,  0, 1.0",
        "1,  1, 2.0",
        "2,  3, 3.5",
        "3,  3, 4.0",
        "5,  3, 6.0",
        "8,  3, 8.0",
        "10, 3, 10.0",
        "11, 3, 10.5",
        "20, 3, 15.0",
        "30, 3, 20.0",
        "35, 3, 20.0",
    })
    void macronGrid_correctMinMax(int annees, int expectedMin, double expectedMax) {
        assertThat(CompensationCalculator.macronMin(annees)).isEqualTo(expectedMin);
        assertThat(CompensationCalculator.macronMax(annees)).isCloseTo(expectedMax, within(0.01));
    }

    // ── Parsing dans CaseAnalysisResponse ────────────────────────────────────

    @Test
    void extractCompensationEstimate_validJson_returnEstimate() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String json = """
                {"compensation_data": {
                  "type_rupture": "LICENCIEMENT",
                  "anciennete_annees": 6,
                  "anciennete_mois": 4,
                  "salaire_reference_mensuel": 2800.0
                }}""";
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
        CompensationCalculator.CompensationEstimate result =
                CaseAnalysisResponse.extractCompensationEstimate(root);
        assertThat(result).isNotNull();
        assertThat(result.typeRupture()).isEqualTo("LICENCIEMENT");
        assertThat(result.ancienneteAnnees()).isEqualTo(6);
    }

    @Test
    void extractCompensationEstimate_missingNode_returnsNull() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree("{}");
        assertThat(CaseAnalysisResponse.extractCompensationEstimate(root)).isNull();
    }

    @Test
    void extractCompensationEstimate_nullNode_returnsNull() throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode root = mapper.readTree("{\"compensation_data\": null}");
        assertThat(CaseAnalysisResponse.extractCompensationEstimate(root)).isNull();
    }
}
