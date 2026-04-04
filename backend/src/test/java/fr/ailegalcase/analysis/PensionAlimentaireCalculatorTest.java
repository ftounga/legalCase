package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class PensionAlimentaireCalculatorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Cas nominal — France, garde exclusive ────────────────────────────────

    @ParameterizedTest
    @CsvSource({
        "1, 2000.0, EXCLUSIVE, FRANCE, 360.0",   // 18 % × 2000
        "2, 2000.0, EXCLUSIVE, FRANCE, 520.0",   // 26 % × 2000
        "3, 2000.0, EXCLUSIVE, FRANCE, 600.0",   // 30 % × 2000
        "4, 2000.0, EXCLUSIVE, FRANCE, 660.0",   // 33 % × 2000
        "5, 2000.0, EXCLUSIVE, FRANCE, 700.0",   // 35 % × 2000
    })
    void france_gardeExclusive_calculCorrect(int nbEnfants, double revenus, String garde, String pays, double expected) {
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> result =
                PensionAlimentaireCalculator.calculate(revenus, nbEnfants, garde, pays);
        assertThat(result).isPresent();
        double montantCentral = Math.round(revenus * getTaux(pays, garde, nbEnfants) * 100.0) / 100.0;
        assertThat(result.get().montantMin()).isCloseTo(montantCentral * 0.90, within(1.0));
        assertThat(result.get().montantMax()).isCloseTo(montantCentral * 1.10, within(1.0));
    }

    @Test
    void france_gardeAlternee_2enfants_calculCorrect() {
        // 16 % × 3000 = 480 → min=432, max=528
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> result =
                PensionAlimentaireCalculator.calculate(3000.0, 2, "ALTERNEE", "FRANCE");
        assertThat(result).isPresent();
        assertThat(result.get().modeGarde()).isEqualTo("ALTERNEE");
        assertThat(result.get().montantMin()).isCloseTo(432.0, within(1.0));
        assertThat(result.get().montantMax()).isCloseTo(528.0, within(1.0));
    }

    @Test
    void belgique_gardeExclusive_2enfants_calculCorrect() {
        // 22 % × 2000 = 440 → min=396, max=484
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> result =
                PensionAlimentaireCalculator.calculate(2000.0, 2, "EXCLUSIVE", "BELGIQUE");
        assertThat(result).isPresent();
        assertThat(result.get().pays()).isEqualTo("BELGIQUE");
        assertThat(result.get().montantMin()).isCloseTo(396.0, within(1.0));
        assertThat(result.get().montantMax()).isCloseTo(484.0, within(1.0));
    }

    // ── Cas données absentes ─────────────────────────────────────────────────

    @Test
    void revenusEtNbEnfantsNull_retourneEmpty() {
        assertThat(PensionAlimentaireCalculator.calculate(null, null, "EXCLUSIVE", "FRANCE")).isEmpty();
    }

    @Test
    void revenusNull_donneesPartielles() {
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> result =
                PensionAlimentaireCalculator.calculate(null, 2, "EXCLUSIVE", "FRANCE");
        assertThat(result).isPresent();
        assertThat(result.get().donneesPartielles()).isTrue();
        assertThat(result.get().montantMin()).isEqualTo(0.0);
    }

    // ── Fail-open ────────────────────────────────────────────────────────────

    @Test
    void modeGardeInconnu_traitECommeExclusive() {
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> exclusive =
                PensionAlimentaireCalculator.calculate(2000.0, 1, "EXCLUSIVE", "FRANCE");
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> inconnu =
                PensionAlimentaireCalculator.calculate(2000.0, 1, "INCONNU", "FRANCE");
        assertThat(exclusive).isPresent();
        assertThat(inconnu).isPresent();
        assertThat(inconnu.get().montantMin()).isEqualTo(exclusive.get().montantMin());
    }

    @Test
    void paysInconnu_traitECommeFrance() {
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> france =
                PensionAlimentaireCalculator.calculate(2000.0, 1, "EXCLUSIVE", "FRANCE");
        Optional<PensionAlimentaireCalculator.PensionAlimentaireEstimate> inconnu =
                PensionAlimentaireCalculator.calculate(2000.0, 1, "EXCLUSIVE", "INCONNU");
        assertThat(france).isPresent();
        assertThat(inconnu).isPresent();
        assertThat(inconnu.get().montantMin()).isEqualTo(france.get().montantMin());
    }

    // ── LegalDomainPromptBuilder ─────────────────────────────────────────────

    @Test
    void promptFamille_contientLes5Champs() {
        String instruction = LegalDomainPromptBuilder.domainSpecificInstruction("DROIT_FAMILLE");
        assertThat(instruction)
                .contains("revenus_net_mensuel_debiteur")
                .contains("revenus_net_mensuel_creancier")
                .contains("nb_enfants")
                .contains("mode_garde")
                .contains("pays_applicable");
    }

    // ── Extraction depuis CaseAnalysisResponse ───────────────────────────────

    @Test
    void extractPensionAlimentaire_jsonComplet_retourneEstimate() throws Exception {
        String json = """
                {"pension_alimentaire_data": {
                  "revenus_net_mensuel_debiteur": 2500.0,
                  "revenus_net_mensuel_creancier": 1200.0,
                  "nb_enfants": 2,
                  "mode_garde": "EXCLUSIVE",
                  "pays_applicable": "FRANCE"
                }}""";
        JsonNode root = MAPPER.readTree(json);
        PensionAlimentaireCalculator.PensionAlimentaireEstimate result =
                CaseAnalysisResponse.extractPensionAlimentaireEstimate(root);
        assertThat(result).isNotNull();
        assertThat(result.nbEnfants()).isEqualTo(2);
        assertThat(result.pays()).isEqualTo("FRANCE");
        assertThat(result.donneesPartielles()).isFalse();
    }

    @Test
    void extractPensionAlimentaire_champAbsent_retourneNull() throws Exception {
        JsonNode root = MAPPER.readTree("{}");
        assertThat(CaseAnalysisResponse.extractPensionAlimentaireEstimate(root)).isNull();
    }

    // ── Utilitaire interne ───────────────────────────────────────────────────

    private double getTaux(String pays, String garde, int nbEnfants) {
        double[][] tauxFR = {{0.18,0.11},{0.26,0.16},{0.30,0.19},{0.33,0.21},{0.35,0.22}};
        double[][] tauxBE = {{0.15,0.09},{0.22,0.14},{0.27,0.17},{0.31,0.19},{0.33,0.21}};
        double[][] taux = "BELGIQUE".equals(pays) ? tauxBE : tauxFR;
        int idx = Math.min(nbEnfants, 5) - 1;
        return "ALTERNEE".equals(garde) ? taux[idx][1] : taux[idx][0];
    }
}
