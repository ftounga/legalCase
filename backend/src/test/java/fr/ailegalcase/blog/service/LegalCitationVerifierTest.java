package fr.ailegalcase.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegalCitationVerifierTest {

    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final BlogPromptBuilder promptBuilder = new BlogPromptBuilder();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LegalCitationVerifier verifier = new LegalCitationVerifier(
            anthropicService, promptBuilder, objectMapper, "claude-haiku-4-5", 1000);

    @Test
    void verify_emptyCitations_skipsCallReturnsEmpty() {
        LegalCitationVerifier.VerifierOutcome outcome = verifier.verify(List.of());

        assertThat(outcome.called()).isFalse();
        assertThat(outcome.hasLikelyHallucination()).isFalse();
        verify(anthropicService, never()).analyzeWithModel(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void verify_zeroHallucination_returnsHasFalse() {
        when(anthropicService.analyzeWithModel(any(), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "{\"verdicts\":[{\"reference\":\"Code du travail, art. L1237-13\"," +
                                "\"verdict\":\"KNOWN\",\"rationale\":\"article existant\"}]}",
                        "claude-haiku-4-5", 200, 50, "end_turn"));

        LegalCitationVerifier.VerifierOutcome outcome = verifier.verify(List.of(
                new GeneratedArticleJson.LegalCitation("Code du travail, art. L1237-13", "ctx")));

        assertThat(outcome.called()).isTrue();
        assertThat(outcome.hasLikelyHallucination()).isFalse();
    }

    @Test
    void verify_oneHallucination_flagsHasHallucination() {
        when(anthropicService.analyzeWithModel(any(), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "{\"verdicts\":[" +
                                "{\"reference\":\"Code du travail, art. L9999\",\"verdict\":\"LIKELY_HALLUCINATION\",\"rationale\":\"inexistant\"}," +
                                "{\"reference\":\"Code civil, art. 270\",\"verdict\":\"KNOWN\",\"rationale\":\"OK\"}" +
                                "]}",
                        "claude-haiku-4-5", 250, 80, "end_turn"));

        LegalCitationVerifier.VerifierOutcome outcome = verifier.verify(List.of(
                new GeneratedArticleJson.LegalCitation("Code du travail, art. L9999", "ctx"),
                new GeneratedArticleJson.LegalCitation("Code civil, art. 270", "ctx2")));

        assertThat(outcome.called()).isTrue();
        assertThat(outcome.hasLikelyHallucination()).isTrue();
    }

    @Test
    void verify_haikuExceptionFallsBackToFailed() {
        when(anthropicService.analyzeWithModel(any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("503 anthropic"));

        LegalCitationVerifier.VerifierOutcome outcome = verifier.verify(List.of(
                new GeneratedArticleJson.LegalCitation("X", "Y")));

        assertThat(outcome.called()).isFalse();
        assertThat(outcome.hasLikelyHallucination()).isFalse();
    }

    @Test
    void stripJsonFences_handlesMarkdownFenced() {
        String input = "```json\n{\"a\":1}\n```";
        assertThat(LegalCitationVerifier.stripJsonFences(input)).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripJsonFences_handlesPlainJson() {
        assertThat(LegalCitationVerifier.stripJsonFences("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void stripJsonFences_handlesNull() {
        assertThat(LegalCitationVerifier.stripJsonFences(null)).isEqualTo("{}");
    }
}
