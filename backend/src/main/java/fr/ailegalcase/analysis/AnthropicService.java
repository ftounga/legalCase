package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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
     * F-120 SF-120-03 — variante avec choix explicite du modèle (utile pour le blog SEO
     * qui appelle Sonnet pour la rédaction et Haiku pour la vérification jurisprudence).
     *
     * @param modelId       identifiant du modèle Anthropic (ex: {@code claude-haiku-4-5})
     * @param systemPrompt  prompt système
     * @param userMessage   message utilisateur
     * @param maxTokens     budget de tokens de sortie
     */
    public AnthropicResult analyzeWithModel(String modelId, String systemPrompt,
                                            String userMessage, int maxTokens) {
        return doAnalyze(modelId, systemPrompt, userMessage, maxTokens, false);
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
     * F-185 SF-185-01 — variante streaming de {@link #analyzeWithSystemCache} qui
     * consomme la SSE Anthropic ({@code stream: true}) et invoque
     * {@code onTextDelta} à chaque chunk de texte reçu (utile pour alimenter
     * un parser JSON incrémental côté caller — cf. {@link PartialJsonSectionExtractor}).
     *
     * <p>La réponse finale agrégée est retournée comme {@link AnthropicResult},
     * identique au mode non-streaming : le caller peut traiter le résultat
     * complet de la même façon.
     *
     * <p>En cas d'erreur réseau ou HTTP en cours de stream, l'exception est
     * propagée — le caller doit décider du fallback (ex. retry ou
     * {@link #analyzeWithSystemCache} synchrone).
     *
     * @param systemPrompt prompt système (cachable)
     * @param userMessage  message utilisateur
     * @param maxTokens    budget de tokens de sortie
     * @param onTextDelta  callback invoqué pour chaque text delta reçu (peut être null)
     */
    public AnthropicResult analyzeWithSystemCacheStreaming(String systemPrompt,
                                                           String userMessage,
                                                           int maxTokens,
                                                           java.util.function.Consumer<String> onTextDelta) {
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be empty");
        }

        Object systemPayload = List.of(Map.of(
                "type", "text",
                "text", systemPrompt,
                "cache_control", Map.of("type", "ephemeral")));

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0,
                "stream", true,
                "system", systemPayload,
                "messages", List.of(Map.of("role", "user", "content", userMessage))
        );

        log.debug("Sending streaming request to Anthropic model {} ({} chars user message)",
                model, userMessage.length());

        StreamAggregator aggregator = new StreamAggregator(onTextDelta);

        restClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(body)
                .exchange((request, response) -> {
                    if (response.getStatusCode().isError()) {
                        throw new HttpServerErrorException(response.getStatusCode(),
                                "Anthropic streaming HTTP error: " + response.getStatusText());
                    }
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            aggregator.consumeLine(line);
                        }
                    }
                    return null;
                });

        log.debug("Anthropic streaming response complete ({} chars text, {} prompt tokens, " +
                        "{} completion tokens, stop_reason={})",
                aggregator.text.length(), aggregator.inputTokens, aggregator.outputTokens, aggregator.stopReason);

        if ("max_tokens".equals(aggregator.stopReason)) {
            log.warn("Anthropic streaming response TRUNCATED — stop_reason=max_tokens, model={}, " +
                            "max_tokens={}, output tokens={}.",
                    model, maxTokens, aggregator.outputTokens);
        }

        return new AnthropicResult(aggregator.text.toString(),
                aggregator.modelUsed != null ? aggregator.modelUsed : model,
                aggregator.inputTokens, aggregator.outputTokens, aggregator.stopReason);
    }

    /**
     * Agrégateur d'état pour la consommation d'un stream SSE Anthropic.
     * Lit ligne par ligne, parse les events {@code data: {...}}, accumule le
     * texte des {@code content_block_delta.delta.text} et capture les usages
     * via {@code message_start.message.usage} et {@code message_delta.usage}.
     */
    private static final class StreamAggregator {
        private static final ObjectMapper MAPPER = new ObjectMapper();

        final StringBuilder text = new StringBuilder();
        final java.util.function.Consumer<String> onTextDelta;
        int inputTokens = 0;
        int outputTokens = 0;
        String stopReason = null;
        String modelUsed = null;

        StreamAggregator(java.util.function.Consumer<String> onTextDelta) {
            this.onTextDelta = onTextDelta;
        }

        void consumeLine(String line) {
            if (line == null || line.isEmpty()) return;
            // SSE format : "data: {...json...}" — on ignore les lignes "event: xxx"
            // (le type est aussi dans le JSON via le champ "type")
            if (!line.startsWith("data: ")) return;
            String payload = line.substring(6).trim();
            if (payload.isEmpty() || "[DONE]".equals(payload)) return;

            try {
                JsonNode evt = MAPPER.readTree(payload);
                String type = evt.path("type").asText("");
                switch (type) {
                    case "message_start" -> {
                        JsonNode msg = evt.path("message");
                        modelUsed = msg.path("model").asText(null);
                        JsonNode usage = msg.path("usage");
                        if (!usage.isMissingNode()) {
                            inputTokens = usage.path("input_tokens").asInt(0);
                            outputTokens = usage.path("output_tokens").asInt(0);
                        }
                    }
                    case "content_block_delta" -> {
                        JsonNode delta = evt.path("delta");
                        if ("text_delta".equals(delta.path("type").asText(""))) {
                            String chunk = delta.path("text").asText("");
                            if (!chunk.isEmpty()) {
                                text.append(chunk);
                                if (onTextDelta != null) {
                                    onTextDelta.accept(chunk);
                                }
                            }
                        }
                    }
                    case "message_delta" -> {
                        JsonNode delta = evt.path("delta");
                        if (delta.has("stop_reason") && !delta.path("stop_reason").isNull()) {
                            stopReason = delta.path("stop_reason").asText(null);
                        }
                        JsonNode usage = evt.path("usage");
                        if (usage.has("output_tokens")) {
                            outputTokens = usage.path("output_tokens").asInt(outputTokens);
                        }
                    }
                    default -> { /* message_stop, ping, content_block_start/stop : ignorés */ }
                }
            } catch (IOException e) {
                // Event mal formé — on continue (fail-open). Le stream peut contenir des
                // events parasites (heartbeats, etc.) qu'on n'a pas à parser.
            }
        }
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
