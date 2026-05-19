package fr.ailegalcase.email;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * F-248 SF-248-01 : endpoints publics de désabonnement des emails non-transactionnels.
 *
 * <p>Sous {@code /api/v1/public/**} — déjà whitelisté par {@code SecurityConfig}
 * ({@code permitAll}). Aucune authentification : le {@code token} fait foi.
 * Aligné sur le pattern du {@code ContactController} public.
 *
 * <p>Contrat de réponse (figé pour SF-248-02) :
 * {@code 200 {"optedOut": boolean}} · {@code 400} token absent/mal formé ·
 * {@code 404} token inconnu (corps {@code {"message": ...}} via le
 * {@code GlobalExceptionHandler}).
 */
@RestController
@RequestMapping("/api/v1/public/email")
public class EmailController {

    private final EmailSubscriptionService emailSubscriptionService;

    public EmailController(EmailSubscriptionService emailSubscriptionService) {
        this.emailSubscriptionService = emailSubscriptionService;
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<Map<String, Boolean>> unsubscribe(@RequestBody EmailSubscriptionRequest request) {
        boolean optedOut = emailSubscriptionService.unsubscribe(request.token());
        return ResponseEntity.ok(Map.of("optedOut", optedOut));
    }

    @PostMapping("/resubscribe")
    public ResponseEntity<Map<String, Boolean>> resubscribe(@RequestBody EmailSubscriptionRequest request) {
        boolean optedOut = emailSubscriptionService.resubscribe(request.token());
        return ResponseEntity.ok(Map.of("optedOut", optedOut));
    }

    @GetMapping("/subscription-status")
    public ResponseEntity<Map<String, Boolean>> subscriptionStatus(@RequestParam(required = false) String token) {
        boolean optedOut = emailSubscriptionService.subscriptionStatus(token);
        return ResponseEntity.ok(Map.of("optedOut", optedOut));
    }
}
