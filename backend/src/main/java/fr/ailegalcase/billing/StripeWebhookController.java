package fr.ailegalcase.billing;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookService webhookService;
    private final String webhookSecret;

    public StripeWebhookController(StripeWebhookService webhookService,
                                   @Value("${app.stripe.webhook-secret:}") String webhookSecret) {
        this.webhookService = webhookService;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(
            @RequestBody byte[] payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = Webhook.constructEvent(new String(payload), sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }

        webhookService.handleEvent(event);
        return ResponseEntity.ok().build();
    }
}
