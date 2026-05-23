package fr.ailegalcase.billing;

import com.stripe.exception.StripeException;
import com.stripe.model.Discount;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceRepository;
import fr.ailegalcase.workspace.WorkspaceStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class StripeWebhookService {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookService.class);

    private final SubscriptionRepository subscriptionRepository;
    private final CreditPurchaseService creditPurchaseService;
    private final WorkspaceRepository workspaceRepository;
    private final PromoCodeRepository promoCodeRepository;
    private final PromoCodeRedemptionRepository promoCodeRedemptionRepository;
    private final String priceIdSolo;
    private final String priceIdTeam;
    private final String priceIdPro;
    private final String priceIdTokens1m;
    private final String priceIdTokens5m;
    private final String priceIdTokens20m;

    public StripeWebhookService(SubscriptionRepository subscriptionRepository,
                                CreditPurchaseService creditPurchaseService,
                                WorkspaceRepository workspaceRepository,
                                PromoCodeRepository promoCodeRepository,
                                PromoCodeRedemptionRepository promoCodeRedemptionRepository,
                                @Value("${app.stripe.price-id-solo:}") String priceIdSolo,
                                @Value("${app.stripe.price-id-team:}") String priceIdTeam,
                                @Value("${app.stripe.price-id-pro:}") String priceIdPro,
                                @Value("${app.stripe.price-id-tokens-1m:}") String priceIdTokens1m,
                                @Value("${app.stripe.price-id-tokens-5m:}") String priceIdTokens5m,
                                @Value("${app.stripe.price-id-tokens-20m:}") String priceIdTokens20m) {
        this.subscriptionRepository = subscriptionRepository;
        this.creditPurchaseService = creditPurchaseService;
        this.workspaceRepository = workspaceRepository;
        this.promoCodeRepository = promoCodeRepository;
        this.promoCodeRedemptionRepository = promoCodeRedemptionRepository;
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
            // SF-156-01 : activation d'un workspace PENDING_PAYMENT à la
            // première création de subscription Stripe.
            case "customer.subscription.created" -> handleSubscriptionCreated(event);
            case "customer.subscription.updated" -> handleSubscriptionUpdated(event);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            // SF-156-01 : un paiement échoué sur un workspace pendant
            // déclenche le passage en CANCELLED.
            case "invoice.payment_failed" -> handleInvoicePaymentFailed(event);
            // SF-255-04 : Stripe émet customer.discount.created lorsqu'un
            // user applique un PromotionCode au Checkout. On synchronise la
            // redemption locale (audit + incrément uses_count).
            case "customer.discount.created" -> handleCustomerDiscountCreated(event);
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
            // SF-247-01 : Stripe émet customer.subscription.updated au moment du
            // cancel_at_period_end ; on synchronise l'état de résiliation local.
            Boolean cancelAtPeriodEnd = stripeSub.getCancelAtPeriodEnd();
            sub.setCancelAtPeriodEnd(Boolean.TRUE.equals(cancelAtPeriodEnd));
            sub.setCurrentPeriodEnd(toInstant(stripeSub.getCurrentPeriodEnd()));
            subscriptionRepository.save(sub);
            log.info("Plan synced to {} (cancelAtPeriodEnd={}) for customer {}",
                    sub.getPlanCode(), sub.isCancelAtPeriodEnd(), customerId);
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
            // SF-156-01 : si le workspace lié est encore PENDING_PAYMENT,
            // l'annulation Stripe signifie que le paiement initial n'a jamais
            // abouti — on bascule en CANCELLED (le cleanup job supprimera).
            Workspace ws = workspaceRepository.findById(sub.getWorkspaceId()).orElse(null);
            if (ws != null && WorkspaceStatus.PENDING_PAYMENT.equals(ws.getStatus())) {
                ws.setStatus(WorkspaceStatus.CANCELLED);
                workspaceRepository.save(ws);
                sub.setStatus("CANCELLED");
                subscriptionRepository.save(sub);
                log.info("Subscription deleted — workspace {} (PENDING_PAYMENT) basculé en CANCELLED",
                        sub.getWorkspaceId());
                return;
            }

            // Cas standard (workspace ACTIVE) : downgrade FREE (comportement
            // historique préservé).
            sub.setPlanCode("FREE");
            sub.setStripeSubscriptionId(null);
            sub.setExpiresAt(Instant.now());
            sub.setStatus("ACTIVE");
            // SF-247-01 : la résiliation programmée est désormais consommée ;
            // on remet l'indicateur à false pour ne pas afficher « résiliation
            // programmée » sur un workspace déjà repassé FREE.
            sub.setCancelAtPeriodEnd(false);
            subscriptionRepository.save(sub);
            log.info("Subscription deleted — workspace downgraded to FREE for customer {}", customerId);
        }, () -> log.warn("No subscription found for Stripe customer {}", customerId));
    }

    /**
     * SF-247-01 : convertit un timestamp Stripe (epoch en secondes) en Instant.
     * Stripe expose current_period_end comme un Long de secondes ; il peut être
     * null si la subscription n'est pas encore complètement initialisée.
     */
    private static Instant toInstant(Long epochSeconds) {
        return epochSeconds == null ? null : Instant.ofEpochSecond(epochSeconds);
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

    /**
     * SF-156-01 — Active un workspace en {@code PENDING_PAYMENT} dès que
     * Stripe confirme la création d'un abonnement.
     *
     * <p>Idempotence (CA5, invariant SF-156-00 §3) : si le workspace est
     * déjà {@code ACTIVE} et que le {@code stripeSubscriptionId} reçu est
     * identique à celui déjà persisté, le handler retourne immédiatement
     * sans effet de bord.
     *
     * <p>Le {@code workspace_id} est récupéré depuis la metadata du Stripe
     * customer (injectée par {@link StripeCheckoutService#createSubscriptionSessionForNewWorkspace}).
     * Si la metadata est absente, on retombe sur la résolution standard
     * par {@code stripeCustomerId} pour ne pas casser l'onboarding initial.
     */
    private void handleSubscriptionCreated(Event event) {
        Subscription stripeSub;
        try {
            stripeSub = (Subscription) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (StripeException e) {
            log.error("Cannot deserialize customer.subscription.created event {}: {}",
                    event.getId(), e.getMessage());
            return;
        }
        if (stripeSub == null) return;

        String customerId = stripeSub.getCustomer();
        String stripeSubscriptionId = stripeSub.getId();
        String priceId = stripeSub.getItems() != null && !stripeSub.getItems().getData().isEmpty()
                ? stripeSub.getItems().getData().get(0).getPrice().getId()
                : null;
        String planCode = resolvePlanCodeFromPriceId(priceId);

        UUID workspaceId = resolveWorkspaceIdFromCustomer(customerId);
        if (workspaceId == null) {
            // Cas onboarding initial (avant SF-156-01) ou customer inconnu :
            // pas d'effet, on log et on laisse handleCheckoutCompleted /
            // handleSubscriptionUpdated faire le reste.
            log.warn("customer.subscription.created reçu pour customer {} sans workspace mappable",
                    customerId);
            return;
        }

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            log.warn("customer.subscription.created : workspace {} introuvable", workspaceId);
            return;
        }

        fr.ailegalcase.billing.Subscription sub = subscriptionRepository
                .findByWorkspaceId(workspaceId).orElse(null);

        // Idempotence : si déjà ACTIVE avec le même subscription id, no-op.
        if (sub != null
                && WorkspaceStatus.ACTIVE.equals(workspace.getStatus())
                && stripeSubscriptionId != null
                && stripeSubscriptionId.equals(sub.getStripeSubscriptionId())) {
            log.debug("customer.subscription.created déjà appliqué (idempotence) pour workspace {}",
                    workspaceId);
            return;
        }

        // Transition PENDING_PAYMENT → ACTIVE (CA5).
        workspace.setStatus(WorkspaceStatus.ACTIVE);
        workspace.setPlanCode(planCode);
        workspaceRepository.save(workspace);

        if (sub == null) {
            sub = new fr.ailegalcase.billing.Subscription();
            sub.setWorkspaceId(workspaceId);
            sub.setStartedAt(Instant.now());
        }
        sub.setPlanCode(planCode);
        sub.setStatus("ACTIVE");
        sub.setStripeCustomerId(customerId);
        sub.setStripeSubscriptionId(stripeSubscriptionId);
        sub.setExpiresAt(null);
        subscriptionRepository.save(sub);
        log.info("Workspace {} activé via customer.subscription.created (plan {}, sub {})",
                workspaceId, planCode, stripeSubscriptionId);
    }

    /**
     * SF-156-01 — Passe un workspace en {@code CANCELLED} si la première
     * facture échoue (paiement refusé) tant qu'il est encore en
     * {@code PENDING_PAYMENT}.
     *
     * <p>Si le workspace est déjà {@code ACTIVE}, on ne le bascule pas en
     * CANCELLED sur un seul échec — Stripe gère le dunning et on attend
     * {@code customer.subscription.deleted} si l'abonnement est résilié.
     */
    private void handleInvoicePaymentFailed(Event event) {
        Invoice invoice;
        try {
            invoice = (Invoice) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (StripeException e) {
            log.error("Cannot deserialize invoice.payment_failed event {}: {}",
                    event.getId(), e.getMessage());
            return;
        }
        if (invoice == null) return;

        String customerId = invoice.getCustomer();
        UUID workspaceId = resolveWorkspaceIdFromCustomer(customerId);
        if (workspaceId == null) {
            log.warn("invoice.payment_failed reçu pour customer {} sans workspace mappable",
                    customerId);
            return;
        }

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            log.warn("invoice.payment_failed : workspace {} introuvable", workspaceId);
            return;
        }

        if (!WorkspaceStatus.PENDING_PAYMENT.equals(workspace.getStatus())) {
            log.debug("invoice.payment_failed ignoré — workspace {} n'est pas PENDING_PAYMENT (status={})",
                    workspaceId, workspace.getStatus());
            return;
        }

        workspace.setStatus(WorkspaceStatus.CANCELLED);
        workspaceRepository.save(workspace);
        subscriptionRepository.findByWorkspaceId(workspaceId).ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            subscriptionRepository.save(sub);
        });
        log.info("Workspace {} basculé en CANCELLED suite à invoice.payment_failed", workspaceId);
    }

    /**
     * SF-255-04 — synchronise une redemption locale lorsque Stripe émet un
     * {@code customer.discount.created} (un user a appliqué un PromotionCode
     * au Checkout).
     *
     * <p>Pipeline :
     * <ol>
     *   <li>Déserialise le {@link Discount} ; ignore si pas lié à un
     *       PromotionCode (discount manuel admin Stripe).</li>
     *   <li>Lookup local par {@code stripe_promotion_code_id} ; warn si
     *       inconnu (cas : code créé hors LegalCase via Stripe Dashboard).</li>
     *   <li>Idempotence : si {@code event.id} déjà persisté en
     *       {@code source_event_id}, no-op (Stripe peut ré-émettre).</li>
     *   <li>Résout {@code workspace_id} via {@code stripe_customer_id} →
     *       Subscription → workspace.</li>
     *   <li>INSERT {@link PromoCodeRedemption} (snapshot complet) +
     *       incrément atomique {@code uses_count}.</li>
     * </ol>
     */
    private void handleCustomerDiscountCreated(Event event) {
        Discount discount;
        try {
            discount = (Discount) event.getDataObjectDeserializer().deserializeUnsafe();
        } catch (StripeException e) {
            log.error("Cannot deserialize customer.discount.created event {}: {}",
                    event.getId(), e.getMessage());
            return;
        }
        if (discount == null) {
            log.warn("Null discount after deserialization for event {}", event.getId());
            return;
        }
        String promotionCodeId = discount.getPromotionCode();
        if (promotionCodeId == null) {
            // Discount appliqué directement (sans passer par un PromotionCode)
            // — par ex. coupon manuel via Dashboard Stripe. Pas de tracking
            // local : ce n'est pas un code F-255.
            log.debug("customer.discount.created sans promotion_code, event={} — ignoré",
                    event.getId());
            return;
        }

        // Idempotence : si l'event a déjà été traité, no-op.
        if (event.getId() != null
                && promoCodeRedemptionRepository.findBySourceEventId(event.getId()).isPresent()) {
            log.info("customer.discount.created déjà traité (idempotence) event={} promoId={}",
                    event.getId(), promotionCodeId);
            return;
        }

        PromoCode promo = promoCodeRepository.findByStripePromotionCodeId(promotionCodeId).orElse(null);
        if (promo == null) {
            log.warn("customer.discount.created pour PromotionCode inconnu localement: {} event={}",
                    promotionCodeId, event.getId());
            return;
        }

        String customerId = discount.getCustomer();
        if (customerId == null) {
            log.warn("customer.discount.created sans customer event={} promoId={}",
                    event.getId(), promotionCodeId);
            return;
        }
        UUID workspaceId = subscriptionRepository.findByStripeCustomerId(customerId)
                .map(fr.ailegalcase.billing.Subscription::getWorkspaceId)
                .orElse(null);
        if (workspaceId == null) {
            log.warn("customer.discount.created pour customer inconnu localement: {} event={}",
                    customerId, event.getId());
            return;
        }

        Workspace workspace = workspaceRepository.findById(workspaceId).orElse(null);
        if (workspace == null) {
            log.warn("customer.discount.created : workspace {} introuvable event={}",
                    workspaceId, event.getId());
            return;
        }

        Integer appliedAmount = null;
        if (discount.getCoupon() != null && discount.getCoupon().getAmountOff() != null) {
            appliedAmount = discount.getCoupon().getAmountOff().intValue();
        }

        UUID appliedByUserId = workspace.getOwner() != null
                ? workspace.getOwner().getId()
                : promo.getCreatedByUserId();

        PromoCodeRedemption redemption = new PromoCodeRedemption();
        redemption.setWorkspaceId(workspaceId);
        redemption.setPromoCodeId(promo.getId());
        redemption.setCodeAtRedemption(promo.getCode());
        redemption.setType(PromoCodeType.STRIPE_DISCOUNT);
        redemption.setValueAppliedAmount(appliedAmount);
        redemption.setAppliedByUserId(appliedByUserId);
        redemption.setSourceEventId(event.getId());
        promoCodeRedemptionRepository.save(redemption);

        // Incrément atomique du compteur. Si le code est épuisé côté local
        // (uses_count >= max_uses) on log warn — Stripe a quand même appliqué
        // la réduction, l'audit reflète la réalité métier.
        int updated = promoCodeRepository.incrementUsesCount(promo.getId());
        if (updated == 0) {
            log.warn("customer.discount.created : promoCode {} déjà à max_uses, "
                    + "redemption {} enregistrée mais uses_count non incrémenté",
                    promo.getCode(), redemption.getId());
        }

        log.info("customer.discount.created traité event={} promoId={} code={} workspaceId={} appliedAmount={}",
                event.getId(), promotionCodeId, promo.getCode(), workspaceId, appliedAmount);
    }

    /**
     * Résout le {@code workspace_id} depuis la metadata du customer Stripe
     * (positionnée par {@link StripeCustomerService#createCustomer}).
     */
    private UUID resolveWorkspaceIdFromCustomer(String customerId) {
        if (customerId == null) return null;
        try {
            com.stripe.model.Customer customer = com.stripe.model.Customer.retrieve(customerId);
            String workspaceIdStr = customer.getMetadata() != null
                    ? customer.getMetadata().get("workspace_id") : null;
            return workspaceIdStr != null ? UUID.fromString(workspaceIdStr) : null;
        } catch (StripeException | IllegalArgumentException e) {
            // Fallback : lookup local via stripe_customer_id.
            return subscriptionRepository.findByStripeCustomerId(customerId)
                    .map(fr.ailegalcase.billing.Subscription::getWorkspaceId)
                    .orElse(null);
        }
    }
}
