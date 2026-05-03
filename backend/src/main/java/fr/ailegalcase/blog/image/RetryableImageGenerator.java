package fr.ailegalcase.blog.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Wrapper retry borné autour de {@link OpenAiImageClient#generate(String)} :
 * 2 retries max (3 appels au total), backoff [500 ms, 2 s], **uniquement** sur
 * erreurs transitoires classifiées par {@link OpenAiErrorClassifier}.
 *
 * <p>Sur erreur définitive ({@code 400}, {@code content_policy_violation},
 * {@code insufficient_quota}) : pas de retry, échec immédiat — évite les retries
 * payés inutiles.
 */
@Component
public class RetryableImageGenerator {

    private static final Logger log = LoggerFactory.getLogger(RetryableImageGenerator.class);

    private final OpenAiImageClient client;
    private final int maxRetries;
    private final long[] backoffMs;

    public RetryableImageGenerator(OpenAiImageClient client,
                                   @Value("${blog.ia.openai.image-retry-max:2}") int maxRetries,
                                   @Value("${blog.ia.openai.image-backoff-ms-1:500}") long backoff1,
                                   @Value("${blog.ia.openai.image-backoff-ms-2:2000}") long backoff2) {
        this.client = client;
        this.maxRetries = maxRetries;
        this.backoffMs = new long[]{backoff1, backoff2};
    }

    /**
     * @return outcome avec image bytes ou raison d'échec ; ne lève jamais d'exception.
     */
    public Outcome generate(String prompt) {
        Throwable lastError = null;
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                OpenAiImageClient.ImageGenerationResponse resp = client.generate(prompt);
                return Outcome.success(resp);
            } catch (Throwable t) {
                lastError = t;
                OpenAiErrorClassifier.Classification cls = OpenAiErrorClassifier.classify(t);
                log.warn("OpenAI image attempt {}/{} failed: {} (class={})",
                        attempt + 1, maxRetries + 1, t.getMessage(), cls);
                if (cls == OpenAiErrorClassifier.Classification.DEFINITIVE) {
                    return Outcome.definitive(t);
                }
                if (attempt < maxRetries) {
                    sleep(backoffMs[Math.min(attempt, backoffMs.length - 1)]);
                }
            }
        }
        return Outcome.transient_(lastError);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public sealed interface Outcome {

        record Success(OpenAiImageClient.ImageGenerationResponse response) implements Outcome {}

        record Failed(boolean wasDefinitive, Throwable cause) implements Outcome {}

        static Outcome success(OpenAiImageClient.ImageGenerationResponse response) {
            return new Success(response);
        }

        static Outcome definitive(Throwable cause) {
            return new Failed(true, cause);
        }

        static Outcome transient_(Throwable cause) {
            return new Failed(false, cause);
        }
    }
}
