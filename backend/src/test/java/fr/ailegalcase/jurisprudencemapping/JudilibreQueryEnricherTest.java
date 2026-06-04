package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * F-JU-06 / SF-JU-06-03 — tests unitaires de {@link JudilibreQueryEnricher}.
 */
class JudilibreQueryEnricherTest {

    private AnthropicService anthropic;
    private JudilibreQueryEnricher enricher;

    @BeforeEach
    void setUp() {
        anthropic = mock(AnthropicService.class);
        enricher = new JudilibreQueryEnricher(anthropic);
    }

    @Test
    void enrich_returnsLlmQuery_normalisingWhitespace() {
        when(anthropic.analyze(any(), any(), any(), anyInt()))
                .thenReturn(result("  indemnité   licenciement\nbarème L1235-3 "));
        String q = enricher.enrich("F-DT-09-comparateur-indemnites", "default", "Comparateur d'indemnités");
        assertThat(q).isEqualTo("indemnité licenciement barème L1235-3");
    }

    @Test
    void enrich_fallsBackToOriginal_whenLlmThrows() {
        when(anthropic.analyze(any(), any(), any(), anyInt())).thenThrow(new RuntimeException("LLM down"));
        assertThat(enricher.enrich("F-DT-09", "default", "Comparateur d'indemnités"))
                .isEqualTo("Comparateur d'indemnités");
    }

    @Test
    void enrich_fallsBackToOriginal_whenLlmBlank() {
        when(anthropic.analyze(any(), any(), any(), anyInt())).thenReturn(result("   "));
        assertThat(enricher.enrich("F-DT-09", "default", "mot-clé original"))
                .isEqualTo("mot-clé original");
    }

    @Test
    void enrich_fallsBackToOriginal_whenResultNull() {
        when(anthropic.analyze(any(), any(), any(), anyInt())).thenReturn(null);
        assertThat(enricher.enrich("F-DT-09", "default", "original")).isEqualTo("original");
    }

    private AnthropicResult result(String content) {
        return new AnthropicResult(content, "claude-test", 0, 0);
    }
}
