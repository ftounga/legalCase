package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClaudeJurisprudenceEvaluatorTest {

    private AnthropicService anthropic;
    private ClaudeJurisprudenceEvaluator evaluator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        anthropic = mock(AnthropicService.class);
        objectMapper = new ObjectMapper();
        evaluator = new ClaudeJurisprudenceEvaluator(anthropic, objectMapper);
    }

    @Test
    void evaluate_emptyCandidates_returnsNone() {
        ClaudeEvaluation result = evaluator.evaluate(buildMapping(), List.of());
        assertThat(result.action()).isEqualTo(EvaluationAction.NONE);
    }

    @Test
    void parseClaudeJson_validConfirmJson_returnsConfirm() {
        String json = "{\"action\":\"CONFIRM\",\"arret_choisi_id\":null,\"confidence_score\":0.92,\"raison\":\"OK\"}";
        ClaudeEvaluation result = evaluator.parseClaudeJson(json, List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.CONFIRM);
        assertThat(result.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.92"));
        assertThat(result.arretChoisi()).isNull();
    }

    @Test
    void parseClaudeJson_validAddJson_resolvesArretById() {
        JudilibreArret c1 = buildArret("AAA");
        JudilibreArret c2 = buildArret("BBB");
        String json = "{\"action\":\"ADD\",\"arret_choisi_id\":\"BBB\",\"confidence_score\":0.88,\"raison\":\"complement\"}";
        ClaudeEvaluation result = evaluator.parseClaudeJson(json, List.of(c1, c2));
        assertThat(result.action()).isEqualTo(EvaluationAction.ADD);
        assertThat(result.arretChoisi()).isSameAs(c2);
    }

    @Test
    void parseClaudeJson_addWithUnknownId_fallbackToNone() {
        String json = "{\"action\":\"ADD\",\"arret_choisi_id\":\"ZZZ\",\"confidence_score\":0.9,\"raison\":\"\"}";
        ClaudeEvaluation result = evaluator.parseClaudeJson(json, List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.NONE);
    }

    @Test
    void parseClaudeJson_invalidJson_fallbackToNone() {
        ClaudeEvaluation result = evaluator.parseClaudeJson("garbage{not json", List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.NONE);
    }

    @Test
    void parseClaudeJson_confidenceClampedAbove1() {
        String json = "{\"action\":\"CONFIRM\",\"confidence_score\":1.5,\"raison\":\"\"}";
        ClaudeEvaluation result = evaluator.parseClaudeJson(json, List.of(buildArret("AAA")));
        assertThat(result.confidenceScore()).isEqualByComparingTo(new BigDecimal("1.00"));
    }

    @Test
    void parseClaudeJson_confidenceClampedBelow0() {
        String json = "{\"action\":\"CONFIRM\",\"confidence_score\":-0.5,\"raison\":\"\"}";
        ClaudeEvaluation result = evaluator.parseClaudeJson(json, List.of(buildArret("AAA")));
        assertThat(result.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    void parseClaudeJson_unknownAction_returnsNone() {
        String json = "{\"action\":\"FOO\",\"confidence_score\":0.9,\"raison\":\"\"}";
        ClaudeEvaluation result = evaluator.parseClaudeJson(json, List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.NONE);
    }

    @Test
    void evaluate_anthropicFails_returnsNone() {
        when(anthropic.analyze(any(), any(), anyInt())).thenThrow(new RuntimeException("anthropic down"));
        ClaudeEvaluation result = evaluator.evaluate(buildMapping(), List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.NONE);
    }

    @Test
    void evaluate_anthropicReturnsValidJson_returnsParsed() {
        when(anthropic.analyze(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "Voici la réponse : {\"action\":\"CONFIRM\",\"confidence_score\":0.95,\"raison\":\"ok\"}",
                        "claude-sonnet", 100, 50));
        ClaudeEvaluation result = evaluator.evaluate(buildMapping(), List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.CONFIRM);
        assertThat(result.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
    }

    private ToolJurisprudenceMapping buildMapping() {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setToolId("f-dt-30");
        m.setBrancheCalculId("anciennete-superieure-10-ans");
        m.setArretRef("Cass. soc. 12 mars 2024, n° 22-XXX");
        m.setJuridiction("Cour de cassation, chambre sociale");
        m.setDateArret(LocalDate.of(2024, 3, 12));
        m.setNumeroPourvoi("22-XXX");
        m.setLienLegifrance("https://www.legifrance.gouv.fr/juri/id/X");
        m.setChapeauOfficiel("Test chapeau.");
        m.setConfidenceScore(new BigDecimal("0.90"));
        return m;
    }

    private JudilibreArret buildArret(String id) {
        return new JudilibreArret(id, "Cass. soc. " + id, "Cour de cassation, chambre sociale",
                LocalDate.of(2025, 1, 8), "23-12.345", "Chapeau " + id,
                "https://www.legifrance.gouv.fr/juri/id/" + id);
    }
}
