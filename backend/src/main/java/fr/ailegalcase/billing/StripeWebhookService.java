package fr.ailegalcase.billing;

import com.stripe.model.Event;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final String priceIdStarter;
    private final String priceIdPro;

    public StripeWebhookService(SubscriptionRepository subscriptionRepository,
                                @Value("${app.stripe.price-id-starter:}") String priceIdStarter,
                                @Value("${app.stripe.price-id-pro:}") String priceIdPro) {
        this.subscriptionRepository = subscriptionRepository;
        this.priceIdStarter = priceIdStarter;
        this.priceIdPro = priceIdPro;
    }

    @Transactional
    public void handleEvent(Event event) {
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> log.debug("Unhandled Stripe event: {}", event.getType());
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = (Session) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (session == null) return;

        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        subscriptionRepository.findByStripeCustomerId(customerId).ifPresentOrElse(sub -> {
            String priceId = session.getMetadata() != null
                    ? session.getMetadata().get("price_id") : null;
            sub.setPlanCode(resolvePlanCode(priceId, subscriptionId, session));
            sub.setStripeSubscriptionId(subscriptionId);
            sub.setExpiresAt(null);
            sub.setStatus("ACTIVE");
            subscriptionRepository.save(sub);
            log.info("Plan updated to {} for customer {}", sub.getPlanCode(), customerId);
        }, () -> log.warn("No subscription found for Stripe customer {}", customerId));
    }

    private void handleSubscriptionUpdated(Event event) {
        Subscription stripeSub = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (stripeSub == null) return;

        String customerId = stripeSub.getCustomer();
        String priceId = stripeSub.getItems().getData().isEmpty() ? null
                : stripeSub.getItems().getData().get(0).getPrice().getId();

        subscriptionRepository.findByStripeCustomerId(customerId).ifPresentOrElse(sub -> {
            sub.setPlanCode(resolvePlanCodeFromPriceId(priceId));
            sub.setStripeSubscriptionId(stripeSub.getId());
            sub.setExpiresAt(null);
            sub.setStatus("ACTIVE");
            subscriptionRepository.save(sub);
            log.info("Plan synced to {} for customer {}", sub.getPlanCode(), customerId);
        }, () -> log.warn("No subscription found for Stripe customer {}", customerId));
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription stripeSub = (Subscription) event.getDataObjectDeserializer()
                .getObject().orElse(null);
        if (stripeSub == null) return;

        String customerId = stripeSub.getCustomer();

        subscriptionRepository.findByStripeCustomerId(customerId).ifPresentOrElse(sub -> {
            sub.setPlanCode("FREE");
            sub.setStripeSubscriptionId(null);
            sub.setExpiresAt(Instant.now());
            sub.setStatus("ACTIVE");
            subscriptionRepository.save(sub);
            log.info("Subscription deleted — workspace downgraded to FREE for customer {}", customerId);
        }, () -> log.warn("No subscription found for Stripe customer {}", customerId));
    }

    private String resolvePlanCode(String priceId, String subscriptionId, Session session) {
        if (priceId != null) return resolvePlanCodeFromPriceId(priceId);
        // Fallback sur metadata de la session
        if (session.getMetadata() != null) {
            String plan = session.getMetadata().get("plan_code");
            if (plan != null) return plan;
        }
        return "STARTER";
    }

    String resolvePlanCodeFromPriceId(String priceId) {
        if (priceId == null) return "STARTER";
        if (priceId.equals(priceIdPro)) return "PRO";
        if (priceId.equals(priceIdStarter)) return "STARTER";
        // fallback si price_id non reconnu
        log.warn("Unknown Stripe price ID: {} — defaulting to STARTER", priceId);
        return "STARTER";
    }
}
