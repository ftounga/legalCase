package fr.ailegalcase.billing;

import com.stripe.exception.StripeException;
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
    private final CreditPurchaseService creditPurchaseService;
    private final String priceIdSolo;
    private final String priceIdTeam;
    private final String priceIdPro;
    private final String priceIdTokens1m;
    private final String priceIdTokens5m;
    private final String priceIdTokens20m;

    public StripeWebhookService(SubscriptionRepository subscriptionRepository,
                                CreditPurchaseService creditPurchaseService,
                                @Value("${app.stripe.price-id-solo:}") String priceIdSolo,
                                @Value("${app.stripe.price-id-team:}") String priceIdTeam,
                                @Value("${app.stripe.price-id-pro:}") String priceIdPro,
                                @Value("${app.stripe.price-id-tokens-1m:}") String priceIdTokens1m,
                                @Value("${app.stripe.price-id-tokens-5m:}") String priceIdTokens5m,
                                @Value("${app.stripe.price-id-tokens-20m:}") String priceIdTokens20m) {
        this.subscriptionRepository = subscriptionRepository;
        this.creditPurchaseService = creditPurchaseService;
        this.priceIdSolo = priceIdSolo;
        this.priceIdTeam = priceIdTeam;
        this.priceIdPro = priceIdPro;
        this.priceIdTokens1m = priceIdTokens1m;
        this.priceIdTokens5m = priceIdTokens5m;
        this.priceIdTokens20m = priceIdTokens20m;
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
        Session session;
        try {
            session = (Session) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (StripeException e) {
            log.error("Cannot deserialize checkout.session.completed event {}: {}", event.getId(), e.getMessage());
            return;
        }
        if (session == null) {
            log.error("Null session after deserialization for event {}", event.getId());
            return;
        }

        if ("payment".equals(session.getMode())) {
            handleTopupPayment(session);
            return;
        }

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

    private void handleTopupPayment(Session session) {
        String workspaceIdStr = session.getMetadata() != null
                ? session.getMetadata().get("workspace_id") : null;
        String packCode = session.getMetadata() != null
                ? session.getMetadata().get("pack_code") : null;
        if (workspaceIdStr == null || packCode == null) {
            log.warn("Topup payment missing metadata in session {}", session.getId());
            return;
        }
        java.util.UUID workspaceId = java.util.UUID.fromString(workspaceIdStr);

        // SF-122-04 : dispatch OCR packs vs token packs
        if (OcrPack.isOcrPack(packCode)) {
            OcrPack ocr;
            try {
                ocr = OcrPack.valueOf(packCode);
            } catch (IllegalArgumentException e) {
                log.warn("Unknown OCR pack_code '{}' in topup session {}", packCode, session.getId());
                return;
            }
            creditPurchaseService.recordOcrPack(workspaceId, ocr.getPages(), ocr.getAmountCents(), session.getId());
            log.info("OCR topup recorded: {} pages for workspace {}", ocr.getPages(), workspaceId);
            return;
        }

        TokenPack pack;
        try {
            pack = TokenPack.valueOf(packCode);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown pack_code '{}' in topup session {}", packCode, session.getId());
            return;
        }
        creditPurchaseService.record(workspaceId, pack.getTokens(), pack.getAmountCents(), session.getId());
        log.info("Topup recorded: {} tokens for workspace {}", pack.getTokens(), workspaceId);
    }

    private void handleSubscriptionUpdated(Event event) {
        Subscription stripeSub;
        try {
            stripeSub = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (StripeException e) {
            log.error("Cannot deserialize customer.subscription.updated event {}: {}", event.getId(), e.getMessage());
            return;
        }
        if (stripeSub == null) return;

        String customerId = stripeSub.getCustomer();
        String priceId = stripeSub.getItems().getData().isEmpty() ? null
                : stripeSub.getItems().getData().get(0).getPrice().getId();
        // SF-123-02 : synchroniser seat_count si la quantity a changé côté Dashboard Stripe.
        Long quantity = stripeSub.getItems().getData().isEmpty() ? null
                : stripeSub.getItems().getData().get(0).getQuantity();

        subscriptionRepository.findByStripeCustomerId(customerId).ifPresentOrElse(sub -> {
            sub.setPlanCode(resolvePlanCodeFromPriceId(priceId));
            sub.setStripeSubscriptionId(stripeSub.getId());
            sub.setExpiresAt(null);
            sub.setStatus("ACTIVE");
            if (quantity != null && quantity > 0 && quantity.intValue() != sub.getSeatCount()) {
                sub.setSeatCount(quantity.intValue());
                log.info("Seat count synced from Stripe to {} for customer {}", quantity, customerId);
            }
            subscriptionRepository.save(sub);
            log.info("Plan synced to {} for customer {}", sub.getPlanCode(), customerId);
        }, () -> log.warn("No subscription found for Stripe customer {}", customerId));
    }

    private void handleSubscriptionDeleted(Event event) {
        Subscription stripeSub;
        try {
            stripeSub = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (StripeException e) {
            log.error("Cannot deserialize customer.subscription.deleted event {}: {}", event.getId(), e.getMessage());
            return;
        }
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
        return "SOLO";
    }

    String resolvePlanCodeFromPriceId(String priceId) {
        if (priceId == null) return "SOLO";
        if (matches(priceId, priceIdPro))  return "PRO";
        if (matches(priceId, priceIdTeam)) return "TEAM";
        if (matches(priceId, priceIdSolo)) return "SOLO";
        log.warn("Unknown Stripe price ID: {} — defaulting to SOLO", priceId);
        return "SOLO";
    }

    private static boolean matches(String priceId, String configured) {
        return configured != null && !configured.isBlank() && configured.equals(priceId);
    }
}
