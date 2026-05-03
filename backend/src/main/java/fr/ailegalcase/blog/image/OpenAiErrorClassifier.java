package fr.ailegalcase.blog.image;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.io.IOException;

/**
 * Classifie les erreurs OpenAI en {@code TRANSIENT} (retry possible) ou
 * {@code DEFINITIVE} (échec immédiat — pas de retry payé).
 *
 * <p>Règles :
 * <ul>
 *   <li>{@code 429}, {@code 5xx}, {@code IOException}, {@code SocketTimeoutException}
 *       → {@code TRANSIENT}</li>
 *   <li>{@code 400}, {@code 401}, {@code 403}, {@code content_policy_violation},
 *       {@code insufficient_quota} → {@code DEFINITIVE}</li>
 * </ul>
 */
public final class OpenAiErrorClassifier {

    public enum Classification {
        TRANSIENT,
        DEFINITIVE
    }

    private OpenAiErrorClassifier() {}

    public static Classification classify(Throwable t) {
        if (t instanceof HttpServerErrorException) {
            return Classification.TRANSIENT;
        }
        if (t instanceof HttpClientErrorException http) {
            int status = http.getStatusCode().value();
            if (status == 429) {
                String body = http.getResponseBodyAsString();
                // insufficient_quota = définitif (cap atteint, retry inutile)
                if (body != null && body.contains("insufficient_quota")) {
                    return Classification.DEFINITIVE;
                }
                return Classification.TRANSIENT;
            }
            return Classification.DEFINITIVE;
        }
        if (t instanceof IOException) {
            return Classification.TRANSIENT;
        }
        // Cause-chain : l'exception RestClient peut wrapper une IOException
        Throwable cause = t.getCause();
        if (cause instanceof IOException) {
            return Classification.TRANSIENT;
        }
        return Classification.DEFINITIVE;
    }
}
