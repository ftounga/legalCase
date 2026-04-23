package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-152 SF-152-01 : tests du parseur + de l'analyzer divorce par consentement mutuel.
 */
class DivorceConsentementScoringTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String raw) throws Exception {
        return MAPPER.readTree(raw);
    }

    @Test
    void U01_sept_OUI_score100_verdictVALIDE() throws Exception {
        JsonNode root = json("""
                {
                  "divorce_consentement_validity_detection": {
                    "DC_MAJORITE":              {"reponse": "OUI", "justification": "majeurs"},
                    "DC_CONSENTEMENT_LIBRE":    {"reponse": "OUI", "justification": "aucun vice"},
                    "DC_CONVENTION_EQUITABLE":  {"reponse": "OUI", "justification": "équitable"},
                    "DC_ENFANT_MINEUR_ENTENDU": {"reponse": "OUI", "justification": "formulaire signé"},
                    "DC_DELAI_REFLEXION_15J":   {"reponse": "OUI", "justification": "20 jours"},
                    "DC_NOTAIRE_DEPOT":         {"reponse": "OUI", "justification": "dépôt acté"},
                    "DC_INDEPENDANCE_AVOCATS":  {"reponse": "OUI", "justification": "2 avocats"}
                  }
                }
                """);
        var detection = CaseAnalysisResponse.extractDivorceConsentementValidityDetection(root);
        var scoring = CaseAnalysisResponse.computeDivorceConsentementScoring(detection);

        assertThat(detection).isNotNull();
        assertThat(scoring).isNotNull();
        assertThat(scoring.score()).isEqualTo(100);
        assertThat(scoring.verdict()).isEqualTo("VALIDE");
        assertThat(scoring.criteresValides()).hasSize(7);
        assertThat(scoring.criteresNonValides()).isEmpty();
        assertThat(scoring.criteresInconnus()).isEmpty();
    }

    @Test
    void U02_troisOUI_quatreNON_score43_verdictRISQUE_ELEVE() throws Exception {
        JsonNode root = json("""
                {
                  "divorce_consentement_validity_detection": {
                    "DC_MAJORITE":              {"reponse": "OUI"},
                    "DC_CONSENTEMENT_LIBRE":    {"reponse": "OUI"},
                    "DC_CONVENTION_EQUITABLE":  {"reponse": "OUI"},
                    "DC_ENFANT_MINEUR_ENTENDU": {"reponse": "NON"},
                    "DC_DELAI_REFLEXION_15J":   {"reponse": "NON"},
                    "DC_NOTAIRE_DEPOT":         {"reponse": "NON"},
                    "DC_INDEPENDANCE_AVOCATS":  {"reponse": "NON"}
                  }
                }
                """);
        var scoring = CaseAnalysisResponse.computeDivorceConsentementScoring(
                CaseAnalysisResponse.extractDivorceConsentementValidityDetection(root));

        assertThat(scoring.score()).isEqualTo(43);
        assertThat(scoring.verdict()).isEqualTo("RISQUE_ELEVE_NULLITE");
        assertThat(scoring.criteresValides()).hasSize(3);
        assertThat(scoring.criteresNonValides()).hasSize(4);
    }

    @Test
    void U03_scoreEntre50_84_verdictRISQUE_MOYEN() throws Exception {
        JsonNode root = json("""
                {
                  "divorce_consentement_validity_detection": {
                    "DC_MAJORITE":              {"reponse": "OUI"},
                    "DC_CONSENTEMENT_LIBRE":    {"reponse": "OUI"},
                    "DC_CONVENTION_EQUITABLE":  {"reponse": "OUI"},
                    "DC_ENFANT_MINEUR_ENTENDU": {"reponse": "OUI"},
                    "DC_DELAI_REFLEXION_15J":   {"reponse": "NON"},
                    "DC_NOTAIRE_DEPOT":         {"reponse": "NON"},
                    "DC_INDEPENDANCE_AVOCATS":  {"reponse": "NON"}
                  }
                }
                """);
        var scoring = CaseAnalysisResponse.computeDivorceConsentementScoring(
                CaseAnalysisResponse.extractDivorceConsentementValidityDetection(root));

        assertThat(scoring.score()).isEqualTo(57);
        assertThat(scoring.verdict()).isEqualTo("RISQUE_MOYEN");
    }

    @Test
    void U04_codeInconnu_skippe() throws Exception {
        JsonNode root = json("""
                {
                  "divorce_consentement_validity_detection": {
                    "CODE_HALLUCINE": {"reponse": "OUI"},
                    "DC_MAJORITE":    {"reponse": "OUI"}
                  }
                }
                """);
        var detection = CaseAnalysisResponse.extractDivorceConsentementValidityDetection(root);
        assertThat(detection).isNotNull();
        assertThat(detection.detections()).hasSize(1);
        assertThat(detection.detections()).containsKey("DC_MAJORITE");
    }

    @Test
    void U05_INCONNU_compteCommeManquant() throws Exception {
        JsonNode root = json("""
                {
                  "divorce_consentement_validity_detection": {
                    "DC_MAJORITE":              {"reponse": "OUI"},
                    "DC_CONSENTEMENT_LIBRE":    {"reponse": "INCONNU"},
                    "DC_CONVENTION_EQUITABLE":  {"reponse": "INCONNU"}
                  }
                }
                """);
        var scoring = CaseAnalysisResponse.computeDivorceConsentementScoring(
                CaseAnalysisResponse.extractDivorceConsentementValidityDetection(root));

        assertThat(scoring.criteresValides()).containsExactly("DC_MAJORITE");
        assertThat(scoring.criteresInconnus()).contains("DC_CONSENTEMENT_LIBRE", "DC_CONVENTION_EQUITABLE");
        // 1 OUI / 7 = 14.29 → arrondi 14
        assertThat(scoring.score()).isEqualTo(14);
    }

    @Test
    void U06_detectionAbsente_scoringNull() throws Exception {
        JsonNode root = json("{}");
        assertThat(CaseAnalysisResponse.extractDivorceConsentementValidityDetection(root)).isNull();
        assertThat(CaseAnalysisResponse.computeDivorceConsentementScoring(null)).isNull();
    }
}
