package fr.ailegalcase.casefile.conclusion;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * F-98 / SF-98-01 — mappe {@link CaseConclusionGuardException} en réponse
 * {@code 409 {"error": "<code>", "message": "<texte avocat>"}}, conformément au
 * contrat API figé de la mini-spec.
 *
 * <p>Advice dédié plutôt qu'ajout au {@code GlobalExceptionHandler} : le champ
 * {@code error} porte ici le <em>code</em> de garde (ex. {@code STAGE_NOT_SET}),
 * pas le statut HTTP — sémantique propre à F-98.</p>
 */
@RestControllerAdvice
public class CaseConclusionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CaseConclusionExceptionHandler.class);

    @ExceptionHandler(CaseConclusionGuardException.class)
    public ResponseEntity<Map<String, String>> handleGuard(CaseConclusionGuardException ex,
                                                           HttpServletRequest request) {
        log.warn("{} {} → 409 {}", request.getMethod(), request.getRequestURI(), ex.getCode());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "error", ex.getCode().name(),
                        "message", ex.getMessage()));
    }
}
