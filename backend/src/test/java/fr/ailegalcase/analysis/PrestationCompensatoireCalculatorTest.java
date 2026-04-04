package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PrestationCompensatoireCalculatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Cas nominal — France ─────────────────────────────────────────────────

    @Test
    void france_donneesCompletes_calculCorrect() {
        // écart = 3000 - 1500 = 1500 | 1500 × 0.30 × 12 × 8 = 43200
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> result =
                PrestationCompensatoireCalculator.calculate(3000.0, 1500.0, 10, "FRANCE");
        assertThat(result).isPresent();
        assertThat(result.get().ecartRevenus()).isCloseTo(1500.0, within(0.01));
        assertThat(result.get().montantMin()).isCloseTo(43200.0 * 0.85, within(1.0));
        assertThat(result.get().montantMax()).isCloseTo(43200.0 * 1.15, within(1.0));
        assertThat(result.get().pays()).isEqualTo("FRANCE");
        assertThat(result.get().donneesPartielles()).isFalse();
    }

    @Test
    void belgique_coeffDifferentDeFrance() {
        // FR : 1500 × 0.30 × 12 × 8 = 43200 | BE : 1500 × 0.25 × 12 × 8 = 36000
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> fr =
                PrestationCompensatoireCalculator.calculate(3000.0, 1500.0, 10, "FRANCE");
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> be =
                PrestationCompensatoireCalculator.calculate(3000.0, 1500.0, 10, "BELGIQUE");
        assertThat(fr).isPresent();
        assertThat(be).isPresent();
        assertThat(be.get().montantMin()).isLessThan(fr.get().montantMin());
        assertThat(be.get().pays()).isEqualTo("BELGIQUE");
    }

    @Test
    void revenusIdentiques_montantsAZero() {
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> result =
                PrestationCompensatoireCalculator.calculate(2000.0, 2000.0, 8, "FRANCE");
        assertThat(result).isPresent();
        assertThat(result.get().ecartRevenus()).isEqualTo(0.0);
        assertThat(result.get().montantMin()).isEqualTo(0.0);
        assertThat(result.get().montantMax()).isEqualTo(0.0);
        assertThat(result.get().donneesPartielles()).isFalse();
    }

    // ── Données absentes / partielles ───────────────────────────────────────

    @Test
    void tousNulls_retourneEmpty() {
        assertThat(PrestationCompensatoireCalculator.calculate(null, null, null, "FRANCE")).isEmpty();
    }

    @Test
    void revenusANull_donneesPartielles() {
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> result =
                PrestationCompensatoireCalculator.calculate(null, 1500.0, 10, "FRANCE");
        assertThat(result).isPresent();
        assertThat(result.get().donneesPartielles()).isTrue();
    }

    @Test
    void dureeNull_donneesPartielles() {
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> result =
                PrestationCompensatoireCalculator.calculate(3000.0, 1500.0, null, "FRANCE");
        assertThat(result).isPresent();
        assertThat(result.get().donneesPartielles()).isTrue();
        assertThat(result.get().dureeMarriage()).isEqualTo(0);
    }

    // ── Fail-open ────────────────────────────────────────────────────────────

    @Test
    void paysInconnu_traitECommeFrance() {
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> france =
                PrestationCompensatoireCalculator.calculate(3000.0, 1500.0, 10, "FRANCE");
        Optional<PrestationCompensatoireCalculator.PrestationCompensatoireEstimate> inconnu =
                PrestationCompensatoireCalculator.calculate(3000.0, 1500.0, 10, "INCONNU");
        assertThat(france).isPresent();
        assertThat(inconnu).isPresent();
        assertThat(inconnu.get().montantMin()).isEqualTo(france.get().montantMin());
    }

    // ── LegalDomainPromptBuilder ─────────────────────────────────────────────

    @Test
    void promptFamille_contientChampsPrestationCompensatoire() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction)
                .contains("prestation_compensatoire_data")
                .contains("revenus_net_mensuel_epoux_a")
                .contains("revenus_net_mensuel_epoux_b")
                .contains("duree_mariage_annees");
    }

    // ── Extraction depuis CaseAnalysisResponse ───────────────────────────────

    @Test
    void extractPrestationCompensatoire_jsonComplet_retourneEstimate() throws Exception {
        String json = """
                {"prestation_compensatoire_data": {
                  "revenus_net_mensuel_epoux_a": 3000.0,
                  "revenus_net_mensuel_epoux_b": 1500.0,
                  "duree_mariage_annees": 12,
                  "nb_enfants_charge": 2,
                  "pays_applicable": "FRANCE"
                }}""";
        JsonNode root = MAPPER.readTree(json);
        PrestationCompensatoireCalculator.PrestationCompensatoireEstimate result =
                CaseAnalysisResponse.extractPrestationCompensatoireEstimate(root);
        assertThat(result).isNotNull();
        assertThat(result.pays()).isEqualTo("FRANCE");
        assertThat(result.donneesPartielles()).isFalse();
    }

    @Test
    void extractPrestationCompensatoire_champAbsent_retourneNull() throws Exception {
        JsonNode root = MAPPER.readTree("{}");
        assertThat(CaseAnalysisResponse.extractPrestationCompensatoireEstimate(root)).isNull();
    }
}
