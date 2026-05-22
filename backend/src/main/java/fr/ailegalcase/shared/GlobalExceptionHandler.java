package fr.ailegalcase.shared;

import fr.ailegalcase.billing.PromoCodeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PaymentRequiredException.class)
    public ResponseEntity<Map<String, String>> handlePaymentRequired(PaymentRequiredException ex,
                                                                     HttpServletRequest request) {
        log.warn("{} {} → 402 {} ({})", request.getMethod(), request.getRequestURI(),
                ex.getMessage(), ex.getCode());
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED)
                .body(Map.of(
                        "error", HttpStatus.PAYMENT_REQUIRED.toString(),
                        "message", ex.getMessage(),
                        "code", ex.getCode().name()));
    }

    /**
     * F-255 SF-255-01 — sérialise les erreurs métier des codes promo selon le
     * même contrat que {@link PaymentRequiredException} (body
     * {@code {error, message, code}}). Le statut HTTP est porté par
     * {@link fr.ailegalcase.billing.PromoCodeErrorCode#status()}.
     */
    @ExceptionHandler(PromoCodeException.class)
    public ResponseEntity<Map<String, String>> handlePromoCode(PromoCodeException ex,
                                                               HttpServletRequest request) {
        HttpStatus status = ex.getCode().status();
        log.warn("{} {} → {} {} ({})", request.getMethod(), request.getRequestURI(),
                status.value(), ex.getMessage(), ex.getCode());
        return ResponseEntity.status(status)
                .body(Map.of(
                        "error", status.toString(),
                        "message", ex.getMessage(),
                        "code", ex.getCode().name()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex,
                                                                    HttpServletRequest request) {
        log.warn("{} {} → {} {}", request.getMethod(), request.getRequestURI(),
                ex.getStatusCode(), ex.getReason());
        return ResponseEntity.status(ex.getStatusCode())
                .body(Map.of("error", ex.getStatusCode().toString(), "message", ex.getReason()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex,
                                                                HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("{} {} → 400 {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Bad Request", "message", message));
    }

    /**
     * Validation au niveau paramètre (@RequestParam / @PathVariable annotés @Size,
     * @Min, etc.) — Spring lève une ConstraintViolationException, qui n'est pas une
     * MethodArgumentNotValidException. Sans ce handler elle remonte en 500. On la
     * traduit en 400, comme la validation de corps de requête.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex,
                                                                          HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .findFirst()
                .orElse("Validation failed");
        log.warn("{} {} → 400 {}", request.getMethod(), request.getRequestURI(), message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Bad Request", "message", message));
    }
}
