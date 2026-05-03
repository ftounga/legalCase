package fr.ailegalcase.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Appelle Claude Haiku pour classifier les citations juridiques produites par
 * Sonnet. Une seule citation {@code LIKELY_HALLUCINATION} fait basculer l'article
 * en {@code NEEDS_REVIEW} (cf. {@link BlogArticleGeneratorService}).
 */
@Component
public class LegalCitationVerifier {

    private static final Logger log = LoggerFactory.getLogger(LegalCitationVerifier.class);

    private final AnthropicService anthropicService;
    private final BlogPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final String haikuModel;
    private final int maxTokens;

    public LegalCitationVerifier(AnthropicService anthropicService,
                                 BlogPromptBuilder promptBuilder,
                                 ObjectMapper objectMapper,
                                 @Value("${blog.ia.haiku.model:claude-haiku-4-5}") String haikuModel,
                                 @Value("${blog.ia.haiku.max-tokens:1000}") int maxTokens) {
        this.anthropicService = anthropicService;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.haikuModel = haikuModel;
        this.maxTokens = maxTokens;
    }

    /**
     * @return verdict + métadonnées de l'appel ; {@link VerifierOutcome#failed()} si
     *         Haiku a échoué (l'article passera en {@code NEEDS_REVIEW}).
     */
    public VerifierOutcome verify(List<GeneratedArticleJson.LegalCitation> citations) {
        if (citations == null || citations.isEmpty()) {
            return VerifierOutcome.empty();
        }

        List<BlogPromptBuilder.LegalCitationToVerify> input = citations.stream()
                .map(c -> new BlogPromptBuilder.LegalCitationToVerify(c.reference(), c.context()))
                .toList();
        BlogPromptBuilder.PromptPair prompts = promptBuilder.buildHaikuPrompt(input);

        try {
            AnthropicResult result = anthropicService.analyzeWithModel(
                    haikuModel, prompts.system(), prompts.user(), maxTokens);
            String json = stripJsonFences(result.content());
            LegalCitationVerdict verdict = objectMapper.readValue(json, LegalCitationVerdict.class);
            boolean hasHallucination = verdict.verdicts() != null && verdict.verdicts().stream()
                    .anyMatch(e -> e.verdict() == LegalCitationVerdict.Outcome.LIKELY_HALLUCINATION);
            return new VerifierOutcome(true, hasHallucination, result, verdict);
        } catch (Exception e) {
            log.warn("Haiku verification failed: {}", e.getMessage());
            return VerifierOutcome.failed();
        }
    }

    static String stripJsonFences(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    public record VerifierOutcome(boolean called,
                                  boolean hasLikelyHallucination,
                                  AnthropicResult anthropicResult,
                                  LegalCitationVerdict verdict) {

        public static VerifierOutcome empty() {
            return new VerifierOutcome(false, false, null, null);
        }

        public static VerifierOutcome failed() {
            return new VerifierOutcome(false, false, null, null);
        }
    }
}
