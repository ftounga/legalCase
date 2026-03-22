package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Profile("local")
public class AnthropicService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicService.class);

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert en droit du travail français.
            Analyse le texte suivant extrait d'un document juridique.
            Identifie et retourne en JSON : les faits, les points juridiques, les risques potentiels, et les questions ouvertes.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            """;

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;

    @Autowired
    public AnthropicService(@Value("${anthropic.api-key}") String apiKey,
                            @Value("${anthropic.model:claude-sonnet-4-6}") String model,
                            RestClient.Builder builder) {
        this.model = model;
        this.restClient = builder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    // Package-private constructor for unit tests
    AnthropicService(String model, RestClient.Builder builder) {
        this.model = model;
        this.restClient = builder.baseUrl("https://api.anthropic.com").build();
    }

    public AnthropicResult analyzeChunk(String chunkText) {
        return analyze(SYSTEM_PROMPT, chunkText, 2048);
    }

    public AnthropicResult analyze(String systemPrompt, String userMessage, int maxTokens) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be empty");
        }

        log.debug("Sending chunk ({} chars) to Anthropic model {}", userMessage.length(), model);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        int[] backoffSeconds = {5, 15, 30, 60};
        for (int attempt = 0; attempt <= backoffSeconds.length; attempt++) {
            try {
                AnthropicResponse response = restClient.post()
                        .uri("/v1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        .body(AnthropicResponse.class);

                String content = response.content().get(0).text();
                log.debug("Anthropic response received ({} chars, {} prompt tokens, {} completion tokens)",
                        content.length(), response.usage().inputTokens(), response.usage().outputTokens());

                return new AnthropicResult(content, response.model(),
                        response.usage().inputTokens(), response.usage().outputTokens());

            } catch (HttpServerErrorException e) {
                boolean retryable = e.getStatusCode().value() == 529 || e.getStatusCode().value() == 529
                        || e.getStatusCode().value() == 503 || e.getStatusCode().value() == 500;
                if (retryable && attempt < backoffSeconds.length) {
                    int wait = backoffSeconds[attempt];
                    log.warn("Anthropic {} — tentative {}/{}, retry dans {}s", e.getStatusCode().value(),
                            attempt + 1, backoffSeconds.length, wait);
                    try { Thread.sleep(wait * 1000L); } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        throw new IllegalStateException("Unreachable");
    }

    private record AnthropicResponse(List<ContentBlock> content, String model, Usage usage) {
        private record ContentBlock(String type, String text) {}
        private record Usage(
                @JsonProperty("input_tokens") int inputTokens,
                @JsonProperty("output_tokens") int outputTokens) {}
    }
}
