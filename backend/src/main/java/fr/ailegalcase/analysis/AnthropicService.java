package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class AnthropicService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicService.class);

    private static final String CHUNK_SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Analyse le texte suivant extrait d'un document juridique.
            Identifie et retourne en JSON : les faits, les points juridiques, les risques potentiels, et les questions ouvertes.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            """;

    private static final String ANTHROPIC_VERSION = "2023-06-01";

    private final RestClient restClient;
    private final String model;
    private final String modelFast;

    @Autowired
    public AnthropicService(@Value("${anthropic.api-key}") String apiKey,
                            @Value("${anthropic.model:claude-sonnet-4-6}") String model,
                            @Value("${anthropic.model-fast:${anthropic.model:claude-sonnet-4-6}}") String modelFast,
                            RestClient.Builder builder) {
        this.model = model;
        this.modelFast = modelFast;
        this.restClient = builder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    // Package-private constructor for unit tests
    AnthropicService(String model, String modelFast, RestClient.Builder builder) {
        this.model = model;
        this.modelFast = modelFast;
        this.restClient = builder.baseUrl("https://api.anthropic.com").build();
    }

    public AnthropicResult analyzeChunk(String chunkText, String legalDomain, String country) {
        String systemPrompt = CHUNK_SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country));
        return analyzeFast(systemPrompt, chunkText, 2048);
    }

    public AnthropicResult analyzeFast(String systemPrompt, String userMessage, int maxTokens) {
        return doAnalyze(modelFast, systemPrompt, userMessage, maxTokens);
    }

    public AnthropicResult analyze(String systemPrompt, String userMessage, int maxTokens) {
        return doAnalyze(model, systemPrompt, userMessage, maxTokens, false);
    }

    /**
     * F-142-04 : variante avec prompt caching Anthropic (cache_control ephemeral).
     * Le system prompt est mis en cache pour 5 min — latence prefill réduite de
     * ~85 % sur les appels suivants avec le même prompt système. Gain important
     * sur les services à gros system prompt (CaseAnalysis, EnrichedAnalysis).
     *
     * <p>Éligibilité : minimum 1024 tokens dans le bloc caché (Sonnet) ou 2048
     * (Haiku). Au-dessous, Anthropic ignore silencieusement le cache_control.
     */
    public AnthropicResult analyzeWithSystemCache(String systemPrompt, String userMessage, int maxTokens) {
        return doAnalyze(model, systemPrompt, userMessage, maxTokens, true);
    }

    /**
     * SF-148-01 : appel multimodal (images + texte) à Claude Vision.
     * Chaque image est encodée base64 PNG. L'ordre des images est préservé,
     * le {@code userText} est appendu en bloc texte final.
     *
     * @param modelId      identifiant du modèle (ex: {@code claude-haiku-4-5-20251001})
     * @param images       liste de bytes PNG (ordre préservé)
     * @param mediaType    MIME type des images (ex: {@code image/png})
     * @param maxTokens    budget de tokens de sortie
     */
    public AnthropicResult analyzeWithImages(String modelId,
                                             String systemPrompt,
                                             List<byte[]> images,
                                             String mediaType,
                                             String userText,
                                             int maxTokens) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("images must not be empty");
        }

        List<Map<String, Object>> content = new ArrayList<>(images.size() + 1);
        for (byte[] img : images) {
            String b64 = java.util.Base64.getEncoder().encodeToString(img);
            content.add(Map.of(
                    "type", "image",
                    "source", Map.of(
                            "type", "base64",
                            "media_type", mediaType,
                            "data", b64)
            ));
        }
        if (userText != null && !userText.isBlank()) {
            content.add(Map.of("type", "text", "text", userText));
        }

        Map<String, Object> body = Map.of(
                "model", modelId,
                "max_tokens", maxTokens,
                "temperature", 0,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", content))
        );

        log.debug("Sending vision request to {} with {} image(s)", modelId, images.size());
        AnthropicResponse response = restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(AnthropicResponse.class);

        String contentText = response.content().get(0).text();
        log.debug("Vision response received ({} chars, {} input tokens, {} output tokens)",
                contentText.length(), response.usage().inputTokens(), response.usage().outputTokens());

        return new AnthropicResult(contentText, response.model(),
                response.usage().inputTokens(), response.usage().outputTokens(), response.stopReason());
    }

    private AnthropicResult doAnalyze(String modelId, String systemPrompt, String userMessage, int maxTokens) {
        return doAnalyze(modelId, systemPrompt, userMessage, maxTokens, false);
    }

    private AnthropicResult doAnalyze(String modelId, String systemPrompt, String userMessage,
                                      int maxTokens, boolean cacheSystem) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be empty");
        }

        log.debug("Sending chunk ({} chars) to Anthropic model {} (cacheSystem={})",
                userMessage.length(), modelId, cacheSystem);

        // F-142-04 : prompt caching — le system prompt peut être envoyé sous forme
        // de tableau avec cache_control: ephemeral pour réutilisation sur 5 min.
        Object systemPayload = cacheSystem
                ? List.of(Map.of(
                        "type", "text",
                        "text", systemPrompt,
                        "cache_control", Map.of("type", "ephemeral")))
                : systemPrompt;

        Map<String, Object> body = Map.of(
                "model", modelId,
                "max_tokens", maxTokens,
                "temperature", 0,
                "system", systemPayload,
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
                String stopReason = response.stopReason();
                log.debug("Anthropic response received ({} chars, {} prompt tokens, {} completion tokens, stop_reason={})",
                        content.length(), response.usage().inputTokens(), response.usage().outputTokens(), stopReason);

                // Détection explicite de la troncature silencieuse : Claude retourne
                // stop_reason="max_tokens" quand il a été coupé en pleine génération.
                // Le JSON / texte est souvent incomplet et non parsable côté caller.
                // Bug historique : AiQuestionService avec max_tokens=1024 sur dossier
                // riche → 0 questions sans aucune trace d'erreur visible.
                if ("max_tokens".equals(stopReason)) {
                    log.warn("Anthropic response TRUNCATED — stop_reason=max_tokens, model={}, max_tokens={}, " +
                                    "output tokens={}. Le caller va probablement voir un JSON/texte incomplet.",
                            modelId, maxTokens, response.usage().outputTokens());
                }

                return new AnthropicResult(content, response.model(),
                        response.usage().inputTokens(), response.usage().outputTokens(), stopReason);

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

    private record AnthropicResponse(
            List<ContentBlock> content,
            String model,
            @JsonProperty("stop_reason") String stopReason,
            Usage usage) {
        private record ContentBlock(String type, String text) {}
        private record Usage(
                @JsonProperty("input_tokens") int inputTokens,
                @JsonProperty("output_tokens") int outputTokens) {}
    }
}
