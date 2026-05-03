package fr.ailegalcase.blog.image;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * F-120 SF-120-04 — client HTTP minimal vers l'API images d'OpenAI.
 *
 * <p>Modèle par défaut : {@code gpt-image-1} (avril 2025), quality {@code medium}.
 * Choix arbitré dans la mini-spec après benchmark vs {@code dall-e-3} :
 * meilleur rapport qualité/prix (~0,037 € vs ~0,073 €), output base64 direct
 * (pas de re-fetch via URL temporaire 1h).
 */
@Component
public class OpenAiImageClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiImageClient.class);

    private final RestClient restClient;
    private final String defaultModel;
    private final String defaultSize;
    private final String defaultQuality;

    @Autowired
    public OpenAiImageClient(@Value("${blog.ia.openai.api-key:}") String apiKey,
                             @Value("${blog.ia.openai.image-model:gpt-image-1}") String defaultModel,
                             @Value("${blog.ia.openai.image-size:1536x1024}") String defaultSize,
                             @Value("${blog.ia.openai.image-quality:medium}") String defaultQuality,
                             RestClient.Builder builder) {
        this.defaultModel = defaultModel;
        this.defaultSize = defaultSize;
        this.defaultQuality = defaultQuality;
        this.restClient = builder
                .baseUrl("https://api.openai.com")
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public ImageGenerationResponse generate(String prompt) {
        Map<String, Object> body = Map.of(
                "model", defaultModel,
                "prompt", prompt,
                "size", defaultSize,
                "quality", defaultQuality,
                "n", 1
        );
        log.debug("Calling OpenAI image generation: model={}, size={}, quality={}",
                defaultModel, defaultSize, defaultQuality);

        OpenAiImageResponse response = restClient.post()
                .uri("/v1/images/generations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(OpenAiImageResponse.class);

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new IllegalStateException("Empty image response from OpenAI");
        }
        OpenAiImageResponse.ImageData data = response.data().get(0);

        byte[] imageBytes;
        if (data.b64Json() != null && !data.b64Json().isBlank()) {
            imageBytes = Base64.getDecoder().decode(data.b64Json());
        } else if (data.url() != null && !data.url().isBlank()) {
            // DALL-E 3 retourne une URL temporaire — on télécharge ici pour
            // garantir une persistence atomique côté caller.
            imageBytes = downloadFromUrl(data.url());
        } else {
            throw new IllegalStateException("Image response without b64_json nor url");
        }

        return new ImageGenerationResponse(imageBytes, defaultModel, prompt);
    }

    private byte[] downloadFromUrl(String url) {
        return restClient.get().uri(url).retrieve().body(byte[].class);
    }

    public record ImageGenerationResponse(byte[] imageBytes, String modelUsed, String promptUsed) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OpenAiImageResponse(List<ImageData> data) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record ImageData(@JsonProperty("b64_json") String b64Json,
                         @JsonProperty("url") String url) {}
    }
}
