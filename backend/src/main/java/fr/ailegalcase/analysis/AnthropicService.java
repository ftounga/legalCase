package fr.ailegalcase.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
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

    public String analyzeChunk(String chunkText) {
        if (chunkText == null || chunkText.isBlank()) {
            throw new IllegalArgumentException("chunkText must not be empty");
        }

        log.debug("Sending chunk ({} chars) to Anthropic model {}", chunkText.length(), model);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 2048,
                "system", SYSTEM_PROMPT,
                "messages", List.of(Map.of("role", "user", "content", chunkText))
        );

        AnthropicResponse response = restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicResponse.class);

        String result = response.content().get(0).text();
        log.debug("Anthropic response received ({} chars)", result.length());
        return result;
    }

    private record AnthropicResponse(List<ContentBlock> content) {
        private record ContentBlock(String type, String text) {}
    }
}
