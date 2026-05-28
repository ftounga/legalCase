package fr.ailegalcase.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.shared.PaymentRequiredCode;
import fr.ailegalcase.shared.PaymentRequiredException;
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
    private final PlanLimitService planLimitService;
    private final UsageEventService usageEventService;

    @Autowired
    public AnthropicService(@Value("${anthropic.api-key}") String apiKey,
                            @Value("${anthropic.model:claude-sonnet-4-6}") String model,
                            @Value("${anthropic.model-fast:${anthropic.model:claude-sonnet-4-6}}") String modelFast,
                            RestClient.Builder builder,
                            PlanLimitService planLimitService,
                            UsageEventService usageEventService) {
        this.model = model;
        this.modelFast = modelFast;
        this.planLimitService = planLimitService;
        this.usageEventService = usageEventService;
        this.restClient = builder
                .baseUrl("https://api.anthropic.com")
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
                .build();
    }

    // Package-private constructor for unit tests — F-257 : prend les mocks gate + record
    AnthropicService(String model, String modelFast, RestClient.Builder builder,
                     PlanLimitService planLimitService, UsageEventService usageEventService) {
        this.model = model;
        this.modelFast = modelFast;
        this.planLimitService = planLimitService;
        this.usageEventService = usageEventService;
        this.restClient = builder.baseUrl("https://api.anthropic.com").build();
    }

    public AnthropicResult analyzeChunk(AiCallContext ctx, String chunkText, String legalDomain, String country) {
        String systemPrompt = CHUNK_SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country));
        return analyzeFast(ctx, systemPrompt, chunkText, 2048);
    }

    public AnthropicResult analyzeFast(AiCallContext ctx, String systemPrompt, String userMessage, int maxTokens) {
        return doAnalyze(ctx, modelFast, systemPrompt, userMessage, maxTokens, false);
    }

    public AnthropicResult analyze(AiCallContext ctx, String systemPrompt, String userMessage, int maxTokens) {
        return doAnalyze(ctx, model, systemPrompt, userMessage, maxTokens, false);
    }

    /**
     * F-120 SF-120-03 — variante avec choix explicite du modèle (utile pour le blog SEO
     * qui appelle Sonnet pour la rédaction et Haiku pour la vérification jurisprudence).
     */
    public AnthropicResult analyzeWithModel(AiCallContext ctx, String modelId, String systemPrompt,
                                            String userMessage, int maxTokens) {
        return doAnalyze(ctx, modelId, systemPrompt, userMessage, maxTokens, false);
    }

    /**
     * F-142-04 : variante avec prompt caching Anthropic (cache_control ephemeral).
     */
    public AnthropicResult analyzeWithSystemCache(AiCallContext ctx, String systemPrompt,
                                                  String userMessage, int maxTokens) {
        return doAnalyze(ctx, model, systemPrompt, userMessage, maxTokens, true);
    }

    /**
     * F-185 SF-185-01 — variante streaming de {@link #analyzeWithSystemCache}.
     * F-257 — gate + record intégrés (gate avant ouverture du stream, record après agrégation).
     */
    public AnthropicResult analyzeWithSystemCacheStreaming(AiCallContext ctx,
                                                           String systemPrompt,
                                                           String userMessage,
                                                           int maxTokens,
                                                           java.util.function.Consumer<String> onTextDelta) {
        if (ctx == null) {
            throw new IllegalArgumentException("AiCallContext must not be null");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be empty");
        }

        checkGate(ctx);

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

        recordUsage(ctx, aggregator.inputTokens, aggregator.outputTokens);

        return new AnthropicResult(aggregator.text.toString(),
                aggregator.modelUsed != null ? aggregator.modelUsed : model,
                aggregator.inputTokens, aggregator.outputTokens, aggregator.stopReason);
    }

    /**
     * Agrégateur d'état pour la consommation d'un stream SSE Anthropic.
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
                // Event mal formé — on continue (fail-open).
            }
        }
    }

    /**
     * SF-148-01 : appel multimodal (images + texte) à Claude Vision.
     * F-257 — gate + record intégrés.
     */
    public AnthropicResult analyzeWithImages(AiCallContext ctx,
                                             String modelId,
                                             String systemPrompt,
                                             List<byte[]> images,
                                             String mediaType,
                                             String userText,
                                             int maxTokens) {
        if (ctx == null) {
            throw new IllegalArgumentException("AiCallContext must not be null");
        }
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("images must not be empty");
        }

        checkGate(ctx);

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

        recordUsage(ctx, response.usage().inputTokens(), response.usage().outputTokens());

        return new AnthropicResult(contentText, response.model(),
                response.usage().inputTokens(), response.usage().outputTokens(), response.stopReason());
    }

    /**
     * F-JU-04 SF-JU-04-02 — appel Anthropic avec le tool natif {@code web_search}.
     * F-257 — gate + record intégrés.
     */
    public AnthropicResult analyzeWithWebSearch(AiCallContext ctx, String systemPrompt, String userMessage,
                                                int maxTokens, int maxWebSearches) {
        if (ctx == null) {
            throw new IllegalArgumentException("AiCallContext must not be null");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be empty");
        }

        checkGate(ctx);

        Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", maxTokens,
                "temperature", 0,
                "system", systemPrompt,
                "tools", List.of(Map.of(
                        "type", "web_search_20250305",
                        "name", "web_search",
                        "max_uses", Math.max(1, Math.min(maxWebSearches, 10))
                )),
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

                StringBuilder textContent = new StringBuilder();
                if (response.content() != null) {
                    for (AnthropicResponse.ContentBlock block : response.content()) {
                        if ("text".equals(block.type()) && block.text() != null) {
                            textContent.append(block.text());
                        }
                    }
                }
                String content = textContent.toString();
                String stopReason = response.stopReason();
                log.debug("Anthropic web_search response received ({} chars text, stop_reason={})",
                        content.length(), stopReason);

                if ("max_tokens".equals(stopReason)) {
                    log.warn("Anthropic web_search response TRUNCATED — model={}, max_tokens={}, output tokens={}",
                            model, maxTokens, response.usage().outputTokens());
                }

                recordUsage(ctx, response.usage().inputTokens(), response.usage().outputTokens());

                return new AnthropicResult(content, response.model(),
                        response.usage().inputTokens(), response.usage().outputTokens(), stopReason);

            } catch (HttpServerErrorException e) {
                boolean retryable = e.getStatusCode().value() == 529
                        || e.getStatusCode().value() == 503
                        || e.getStatusCode().value() == 500;
                if (retryable && attempt < backoffSeconds.length) {
                    int wait = backoffSeconds[attempt];
                    log.warn("Anthropic web_search {} — tentative {}/{}, retry dans {}s",
                            e.getStatusCode().value(), attempt + 1, backoffSeconds.length, wait);
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

    private AnthropicResult doAnalyze(AiCallContext ctx, String modelId, String systemPrompt,
                                      String userMessage, int maxTokens, boolean cacheSystem) {
        if (ctx == null) {
            throw new IllegalArgumentException("AiCallContext must not be null");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be empty");
        }

        checkGate(ctx);

        log.debug("Sending chunk ({} chars) to Anthropic model {} (cacheSystem={})",
                userMessage.length(), modelId, cacheSystem);

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

                if ("max_tokens".equals(stopReason)) {
                    log.warn("Anthropic response TRUNCATED — stop_reason=max_tokens, model={}, max_tokens={}, " +
                                    "output tokens={}. Le caller va probablement voir un JSON/texte incomplet.",
                            modelId, maxTokens, response.usage().outputTokens());
                }

                recordUsage(ctx, response.usage().inputTokens(), response.usage().outputTokens());

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

    /**
     * F-257 — gate token. Levée d'une {@link PaymentRequiredException} avant tout appel HTTP
     * Anthropic si le workspace user-level est au-delà de son budget mensuel.
     * Pour les contextes system-level, le gate est skip (mais le record reste obligatoire).
     */
    private void checkGate(AiCallContext ctx) {
        if (ctx.jobType().isSystemLevel()) {
            return;
        }
        if (planLimitService.isMonthlyTokenBudgetExceeded(ctx.workspaceId())) {
            log.warn("Monthly token budget exceeded for workspace {} — Anthropic call refused (jobType={}, caseFileId={})",
                    ctx.workspaceId(), ctx.jobType(), ctx.caseFileId());
            throw new PaymentRequiredException(PaymentRequiredCode.TOKEN_BUDGET_EXCEEDED,
                    "Budget tokens mensuel dépassé.");
        }
    }

    /**
     * F-257 — enregistrement automatique de la consommation après réponse Anthropic.
     * Pour les contextes system-level, {@code userId} est null (autorisé côté
     * {@link UsageEventService#record}).
     */
    private void recordUsage(AiCallContext ctx, int tokensInput, int tokensOutput) {
        usageEventService.record(ctx.caseFileId(), ctx.userId(), ctx.jobType(), tokensInput, tokensOutput);
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
