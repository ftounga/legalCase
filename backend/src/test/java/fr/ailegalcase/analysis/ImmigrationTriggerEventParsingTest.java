package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.immigration.ImmigrationTriggerEvent;
import fr.ailegalcase.immigration.ImmigrationTriggerEventCode;
import fr.ailegalcase.immigration.ImmigrationTriggerEventReferential;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-150 SF-150-01 : tests du parseur {@code extractImmigrationTriggerEvents}
 * de {@link CaseAnalysisResponse}.
 */
class ImmigrationTriggerEventParsingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String raw) throws Exception {
        return MAPPER.readTree(raw);
    }

    @Test
    void U01_événementCompletEnrichiAvecBaseLegaleEtTitreSuggere() throws Exception {
        JsonNode root = json("""
                {
                  "trigger_events": [{
                    "event_code": "MARIAGE_RESSORTISSANT_FR",
                    "event_date": "2025-03-15",
                    "source_document": "acte_mariage_n127.pdf",
                    "justification": "Mariage célébré le 15/03/2025 mentionné dans l'acte n°127"
                  }]
                }
                """);
        List<ImmigrationTriggerEvent> events = CaseAnalysisResponse.extractImmigrationTriggerEvents(root);

        assertThat(events).hasSize(1);
        ImmigrationTriggerEvent e = events.get(0);
        assertThat(e.eventCode()).isEqualTo("MARIAGE_RESSORTISSANT_FR");
        assertThat(e.eventLabel()).isEqualTo("Mariage célébré ou publié avec un ressortissant français");
        assertThat(e.eventDate()).isEqualTo("2025-03-15");
        assertThat(e.sourceDocument()).isEqualTo("acte_mariage_n127.pdf");
        assertThat(e.justification()).contains("15/03/2025");
        assertThat(e.baseLegale()).contains("L.423-1");
        assertThat(e.suggestedTitleCode()).isEqualTo("CST_VPF");
        assertThat(e.suggestedTitleLabel()).contains("Vie privée et familiale");
    }

    @Test
    void U02_plusieursEvenements_ordrePreserve() throws Exception {
        JsonNode root = json("""
                {
                  "trigger_events": [
                    {"event_code": "DOCTORAT_OBTENU", "event_date": "2026-10-15", "source_document": "attestation_these.pdf", "justification": "Soutenance prévue octobre 2026"},
                    {"event_code": "MARIAGE_RESSORTISSANT_FR", "event_date": "2025-03-15", "source_document": "acte_mariage.pdf", "justification": "Mariage acte n°127"}
                  ]
                }
                """);
        List<ImmigrationTriggerEvent> events = CaseAnalysisResponse.extractImmigrationTriggerEvents(root);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).eventCode()).isEqualTo("DOCTORAT_OBTENU");
        assertThat(events.get(0).baseLegale()).contains("L.421-14");
        assertThat(events.get(0).suggestedTitleCode()).isEqualTo("CARTE_PLURIANNUELLE");
        assertThat(events.get(1).eventCode()).isEqualTo("MARIAGE_RESSORTISSANT_FR");
    }

    @Test
    void U03_tableauAbsent_listeVide() throws Exception {
        JsonNode root = json("""
                { "faits": [], "points_juridiques": [] }
                """);
        assertThat(CaseAnalysisResponse.extractImmigrationTriggerEvents(root)).isEmpty();
    }

    @Test
    void U03bis_tableauVide_listeVide() throws Exception {
        JsonNode root = json("""
                { "trigger_events": [] }
                """);
        assertThat(CaseAnalysisResponse.extractImmigrationTriggerEvents(root)).isEmpty();
    }

    @Test
    void U04_codeInconnu_skippeSilencieusement() throws Exception {
        JsonNode root = json("""
                {
                  "trigger_events": [
                    {"event_code": "HALLUCINATION_IA", "event_date": "2025-01-01", "source_document": "x.pdf"},
                    {"event_code": "MARIAGE_RESSORTISSANT_FR", "event_date": "2025-03-15", "source_document": "acte.pdf"}
                  ]
                }
                """);
        List<ImmigrationTriggerEvent> events = CaseAnalysisResponse.extractImmigrationTriggerEvents(root);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).eventCode()).isEqualTo("MARIAGE_RESSORTISSANT_FR");
    }

    @Test
    void U05_eventDateAbsent_champNull() throws Exception {
        JsonNode root = json("""
                {
                  "trigger_events": [
                    {"event_code": "CDI_OBTENU_SALARIE", "source_document": "contrat.pdf", "justification": "CDI signé mais date absente"}
                  ]
                }
                """);
        List<ImmigrationTriggerEvent> events = CaseAnalysisResponse.extractImmigrationTriggerEvents(root);
        assertThat(events).hasSize(1);
        assertThat(events.get(0).eventDate()).isNull();
        assertThat(events.get(0).baseLegale()).contains("L.421-1");
    }

    @Test
    void U06_referentielResoudLes10Codes() {
        for (ImmigrationTriggerEventCode code : ImmigrationTriggerEventCode.values()) {
            var def = ImmigrationTriggerEventReferential.resolve(code);
            assertThat(def).as("référentiel doit avoir une définition pour " + code).isPresent();
            assertThat(def.get().baseLegale()).isNotBlank();
            assertThat(def.get().suggestedTitleCode()).isNotBlank();
            assertThat(def.get().suggestedTitleLabel()).isNotBlank();
            assertThat(def.get().eventLabel()).isNotBlank();
        }
    }

    @Test
    void U07_parseCode_casseMixtePuisTrim() {
        assertThat(ImmigrationTriggerEventReferential.parseCode("mariage_ressortissant_fr")).isPresent();
        assertThat(ImmigrationTriggerEventReferential.parseCode(" CDI_OBTENU_SALARIE ")).isPresent();
        assertThat(ImmigrationTriggerEventReferential.parseCode(null)).isEmpty();
        assertThat(ImmigrationTriggerEventReferential.parseCode("")).isEmpty();
        assertThat(ImmigrationTriggerEventReferential.parseCode("UNKNOWN")).isEmpty();
    }
}
