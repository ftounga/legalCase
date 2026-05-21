package fr.ailegalcase.observability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload envoyé par le frontend pour signaler une erreur runtime JavaScript.
 *
 * <p>SF-INFRA-09 — remplace Sentry par une chaîne CloudWatch native :
 * frontend Angular ErrorHandler/HttpInterceptor → POST /api/v1/logs/client-error
 * → SLF4J log.error → stdout pod → Fluent Bit → CloudWatch.</p>
 */
public record ClientErrorPayload(
        @NotBlank
        @Size(max = 500)
        String message,

        @Size(max = 4000)
        String stack,

        @Size(max = 500)
        String url,

        @Size(max = 200)
        String userAgent,

        @Size(max = 50)
        String timestamp,

        @Size(max = 50)
        String appVersion
) {
}
