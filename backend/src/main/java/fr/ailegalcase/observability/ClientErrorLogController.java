package fr.ailegalcase.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de collecte des erreurs frontend.
 *
 * <p>Auth optionnelle (capte aussi les erreurs pré-login). Rate limit
 * 10/min par user, 5/min par IP anonyme — applique le {@link ClientErrorLogService}.</p>
 *
 * <p>SF-INFRA-09.</p>
 */
@RestController
@RequestMapping("/api/v1/logs")
public class ClientErrorLogController {

    private static final Logger log = LoggerFactory.getLogger(ClientErrorLogController.class);

    private final ClientErrorLogService service;

    public ClientErrorLogController(ClientErrorLogService service) {
        this.service = service;
    }

    @PostMapping("/client-error")
    public ResponseEntity<Map<String, String>> log(
            @Valid @RequestBody ClientErrorPayload payload,
            @AuthenticationPrincipal OAuth2User principal,
            HttpServletRequest request) {
        try {
            String userKey = principal != null ? principal.getName() : null;
            String ipKey = userKey == null ? resolveIp(request) : null;
            boolean accepted = service.tryLog(payload, userKey, ipKey);
            if (!accepted) {
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(Map.of("status", "rate_limited"));
            }
            return ResponseEntity.ok(Map.of("status", "logged"));
        } catch (Exception ex) {
            log.warn("Failed to log frontend client error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("status", "error"));
        }
    }

    private static String resolveIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int comma = xff.indexOf(',');
            return (comma > 0 ? xff.substring(0, comma) : xff).trim();
        }
        return request.getRemoteAddr();
    }
}
