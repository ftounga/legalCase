package fr.ailegalcase.jurisprudencemapping;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AiCallContext;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
        when(anthropic.analyze(any(AiCallContext.class), any(), any(), anyInt())).thenThrow(new RuntimeException("anthropic down"));
        ClaudeEvaluation result = evaluator.evaluate(buildMapping(), List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.NONE);
    }

    @Test
    void evaluate_anthropicReturnsValidJson_returnsParsed() {
        when(anthropic.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "Voici la réponse : {\"action\":\"CONFIRM\",\"confidence_score\":0.95,\"raison\":\"ok\"}",
                        "claude-sonnet", 100, 50));
        ClaudeEvaluation result = evaluator.evaluate(buildMapping(), List.of(buildArret("AAA")));
        assertThat(result.action()).isEqualTo(EvaluationAction.CONFIRM);
        assertThat(result.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.95"));
    }

    // --- SF-JU-01-08 — bascule mode bootstrap vs mode dérive ---

    @Test
    void evaluate_realMapping_usesDerivePrompt() {
        when(anthropic.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "{\"action\":\"CONFIRM\",\"confidence_score\":0.9,\"raison\":\"ok\"}",
                        "claude-sonnet", 100, 50));

        evaluator.evaluate(buildMapping(), List.of(buildArret("AAA")));

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropic).analyze(any(AiCallContext.class), systemCaptor.capture(), any(), eq(600));
        String systemPrompt = systemCaptor.getValue();
        assertThat(systemPrompt).contains("CONFIRM, ADD, REPLACE, ARCHIVE, NONE");
        assertThat(systemPrompt).doesNotContain("bootstrap initial");
    }

    @Test
    void evaluate_bootstrapMapping_usesBootstrapPrompt() {
        when(anthropic.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "{\"action\":\"ADD\",\"arret_choisi_id\":\"AAA\",\"confidence_score\":0.85,\"raison\":\"structurant\"}",
                        "claude-sonnet", 100, 50));

        evaluator.evaluate(buildBootstrapMapping(), List.of(buildArret("AAA")));

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropic).analyze(any(AiCallContext.class), systemCaptor.capture(), any(), eq(600));
        String systemPrompt = systemCaptor.getValue();
        assertThat(systemPrompt).contains("bootstrap initial");
        assertThat(systemPrompt).contains("Actions autorisées : ADD ou NONE uniquement");
        assertThat(systemPrompt).doesNotContain("REPLACE");
        assertThat(systemPrompt).doesNotContain("ARCHIVE");
        // SF-JU-01-11 — durcissement format JSON-only
        assertThat(systemPrompt).contains("RÈGLE DE FORMAT ABSOLUE");
        assertThat(systemPrompt).contains("PAS de préambule");
        assertThat(systemPrompt).contains("PAS de balises markdown");
        // Au moins un exemple JSON inline (sert de few-shot)
        assertThat(systemPrompt).contains("{\"action\":\"ADD\"");
        assertThat(systemPrompt).contains("{\"action\":\"NONE\"");
        // SF-JU-06-01 — biais INVERSÉ : « silence > erreur », NONE privilégié en cas de doute
        assertThat(systemPrompt).contains("silence > erreur");
        assertThat(systemPrompt).contains("EN CAS DE DOUTE → NONE");
        assertThat(systemPrompt).contains("FONDE RÉELLEMENT");
    }

    @Test
    void evaluate_bootstrapMapping_addReply_returnsArret() {
        JudilibreArret picked = buildArret("BBB");
        when(anthropic.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "{\"action\":\"ADD\",\"arret_choisi_id\":\"BBB\",\"confidence_score\":0.9,\"raison\":\"structurant\"}",
                        "claude-sonnet", 100, 50));

        ClaudeEvaluation result = evaluator.evaluate(
                buildBootstrapMapping(),
                List.of(buildArret("AAA"), picked));

        assertThat(result.action()).isEqualTo(EvaluationAction.ADD);
        assertThat(result.arretChoisi()).isSameAs(picked);
        assertThat(result.confidenceScore()).isEqualByComparingTo(new BigDecimal("0.90"));
    }

    @Test
    void evaluate_bootstrapMapping_noneReply_returnsNone() {
        when(anthropic.analyze(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "{\"action\":\"NONE\",\"arret_choisi_id\":null,\"confidence_score\":0.4,\"raison\":\"aucun candidat pertinent\"}",
                        "claude-sonnet", 100, 50));

        ClaudeEvaluation result = evaluator.evaluate(
                buildBootstrapMapping(),
                List.of(buildArret("AAA")));

        assertThat(result.action()).isEqualTo(EvaluationAction.NONE);
        assertThat(result.arretChoisi()).isNull();
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

    private ToolJurisprudenceMapping buildBootstrapMapping() {
        ToolJurisprudenceMapping m = new ToolJurisprudenceMapping();
        m.setToolId("f-dt-99-test");
        m.setBrancheCalculId("branche-bootstrap-test");
        m.setArretRef(JurisprudenceBootstrapService.BOOTSTRAP_MAPPING_REF_PREFIX + " — pas de mapping actuel)");
        m.setJuridiction("");
        m.setDateArret(LocalDate.of(2026, 5, 27));
        m.setNumeroPourvoi("");
        m.setLienLegifrance("");
        m.setChapeauOfficiel("Recherche : mot-clé test");
        m.setConfidenceScore(new BigDecimal("0.00"));
        return m;
    }

    private JudilibreArret buildArret(String id) {
        return new JudilibreArret(id, "Cass. soc. " + id, "Cour de cassation, chambre sociale",
                LocalDate.of(2025, 1, 8), "23-12.345", "Chapeau " + id,
                "https://www.legifrance.gouv.fr/juri/id/" + id);
    }
}
