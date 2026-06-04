package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-JU-06 / SF-JU-06-01 — tests unitaires de {@link JurisprudenceRelevanceGate}.
 */
class JurisprudenceRelevanceGateTest {

    private AnthropicService anthropic;
    private JurisprudenceRelevanceGate gate;

    @BeforeEach
    void setUp() {
        anthropic = mock(AnthropicService.class);
        gate = new JurisprudenceRelevanceGate(anthropic, new ObjectMapper());
    }

    @Test
    void parse_pertinentTrue() {
        var v = gate.parse("{\"pertinent\": true, \"raison\": \"fonde directement le sujet\"}");
        assertThat(v.pertinent()).isTrue();
        assertThat(v.raison()).isEqualTo("fonde directement le sujet");
    }

    @Test
    void parse_pertinentFalse() {
        var v = gate.parse("{\"pertinent\": false, \"raison\": \"hors-sujet\"}");
        assertThat(v.pertinent()).isFalse();
    }

    @Test
    void parse_extractsJsonEvenWithSurroundingText() {
        var v = gate.parse("Voici ma réponse : {\"pertinent\": true, \"raison\": \"ok\"} merci");
        assertThat(v.pertinent()).isTrue();
    }

    @Test
    void parse_invalidJson_defaultsToNotPertinent() {
        // silence > erreur : tout ce qui n'est pas parsable est rejeté
        assertThat(gate.parse("pas du json").pertinent()).isFalse();
        assertThat(gate.parse("").pertinent()).isFalse();
    }

    @Test
    void parse_missingField_defaultsToFalse() {
        assertThat(gate.parse("{\"raison\":\"pas de champ pertinent\"}").pertinent()).isFalse();
    }

    @Test
    void assess_nullArret_isNotPertinent() {
        assertThat(gate.assess("sujet", null).pertinent()).isFalse();
    }

    @Test
    void assess_anthropicThrows_defaultsToNotPertinent() {
        when(anthropic.analyze(any(), any(), any(), anyInt())).thenThrow(new RuntimeException("LLM down"));
        var v = gate.assess("Comparateur d'indemnités de licenciement", arret("Chapeau pertinent"));
        assertThat(v.pertinent()).isFalse();
    }

    @Test
    void assess_emptyResponse_isNotPertinent() {
        when(anthropic.analyze(any(), any(), any(), anyInt())).thenReturn(result(""));
        assertThat(gate.assess("sujet", arret("chapeau")).pertinent()).isFalse();
    }

    @Test
    void assess_pertinentResponse_returnsTrue() {
        when(anthropic.analyze(any(), any(), any(), anyInt()))
                .thenReturn(result("{\"pertinent\": true, \"raison\": \"arrêt de principe applicable\"}"));
        var v = gate.assess("Comparateur d'indemnités de licenciement", arret("Barème Macron art. L1235-3"));
        assertThat(v.pertinent()).isTrue();
    }

    @Test
    void assess_offTopicResponse_returnsFalse() {
        when(anthropic.analyze(any(), any(), any(), anyInt()))
                .thenReturn(result("{\"pertinent\": false, \"raison\": \"prime d'ancienneté convention ferroviaire, hors-sujet\"}"));
        var v = gate.assess("Comparateur d'indemnités de licenciement", arret("Restauration ferroviaire — prime d'ancienneté"));
        assertThat(v.pertinent()).isFalse();
    }

    private AnthropicResult result(String content) {
        return new AnthropicResult(content, "claude-test", 0, 0);
    }

    private JudilibreArret arret(String chapeau) {
        return new JudilibreArret("id-1", "Cass. soc. 11 mai 2022, n° 21-14.490",
                "Cour de cassation, chambre sociale", LocalDate.of(2022, 5, 11),
                "21-14.490", chapeau, "https://www.legifrance.gouv.fr/juri/id/X");
    }
}
