package fr.ailegalcase.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Set;

@Service
public class StripeCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutService.class);
    private static final Set<String> VALID_PLANS = Set.of("SOLO", "TEAM", "PRO");

    private final boolean stripeEnabled;
    private final String secretKey;
    private final String priceIdSolo;
    private final String priceIdTeam;
    private final String priceIdPro;
    private final String frontendUrl;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeCustomerService stripeCustomerService;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public StripeCheckoutService(
            @Value("${app.stripe.enabled:false}") boolean stripeEnabled,
            @Value("${app.stripe.secret-key:}") String secretKey,
            @Value("${app.stripe.price-id-solo:}") String priceIdSolo,
            @Value("${app.stripe.price-id-team:}") String priceIdTeam,
            @Value("${app.stripe.price-id-pro:}") String priceIdPro,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            SubscriptionRepository subscriptionRepository,
            StripeCustomerService stripeCustomerService,
            CurrentUserResolver currentUserResolver,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.stripeEnabled = stripeEnabled;
        this.secretKey = secretKey;
        this.priceIdSolo = priceIdSolo;
        this.priceIdTeam = priceIdTeam;
        this.priceIdPro = priceIdPro;
        this.frontendUrl = frontendUrl;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeCustomerService = stripeCustomerService;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public String createCheckoutSession(String planCode, OidcUser oidcUser, String provider, Principal principal) {
        if (!stripeEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe is not enabled");
        }
        if (!VALID_PLANS.contains(planCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid plan code: " + planCode);
        }

        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        Workspace workspace = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();

        fr.ailegalcase.billing.Subscription sub = subscriptionRepository
                .findByWorkspaceId(workspace.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        // Ensure stripe customer exists
        if (sub.getStripeCustomerId() == null) {
            stripeCustomerService.createCustomer(user.getEmail(), workspace.getId())
                    .ifPresent(customerId -> {
                        sub.setStripeCustomerId(customerId);
                        subscriptionRepository.save(sub);
                    });
        }

        String priceId = resolvePriceId(planCode);
        if (priceId == null || priceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Price ID non configuré pour le plan " + planCode
                    + " (variable d'environnement STRIPE_PRICE_ID_" + planCode + " manquante)");
        }

        try {
            Stripe.apiKey = secretKey;
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    // SF-255-04 — active la saisie native d'un code promo
                    // STRIPE_DISCOUNT dans Stripe Checkout. Aucun impact pour
                    // les users qui ne saisissent pas de code (champ optionnel
                    // côté Stripe).
                    .setAllowPromotionCodes(true)
                    .setSuccessUrl(frontendUrl + "/workspace/billing?success=true")
                    .setCancelUrl(frontendUrl + "/workspace/billing?canceled=true")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("plan_code", planCode)
                    .putMetadata("workspace_id", workspace.getId().toString());

            if (sub.getStripeCustomerId() != null) {
                params.setCustomer(sub.getStripeCustomerId());
            }

            Session session = Session.create(params.build());
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session for workspace {}: {}", workspace.getId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment service unavailable");
        }
    }

    String resolvePriceId(String planCode) {
        return switch (planCode) {
            case "PRO"  -> priceIdPro;
            case "TEAM" -> priceIdTeam;
            case "SOLO" -> priceIdSolo;
            default     -> null;
        };
    }

    /**
     * SF-156-01 : crée une session Stripe Checkout pour l'activation d'un
     * <strong>nouveau workspace</strong> en état {@code PENDING_PAYMENT}.
     *
     * <p>Diffère de {@link #createCheckoutSession} qui cible le workspace
     * primary de l'utilisateur : ici on cible explicitement {@code newWorkspaceId}
     * et on injecte cet ID dans la metadata Stripe pour que le webhook
     * {@code customer.subscription.created} sache à quel workspace appliquer
     * la transition {@code PENDING_PAYMENT → ACTIVE}.
     *
     * <p>Comportement :
     * <ul>
     *   <li>Stripe désactivé → retourne {@link java.util.Optional#empty()},
     *       le service appelant doit activer le workspace immédiatement
     *       (mode dev / test).</li>
     *   <li>Plan invalide → {@code 400}.</li>
     *   <li>Price ID non configuré → {@code 503}.</li>
     *   <li>Erreur Stripe (API down, etc.) → propage une
     *       {@link org.springframework.web.server.ResponseStatusException}
     *       en {@code 502} pour que la transaction appelante rollback.</li>
     * </ul>
     *
     * @param planCode "TEAM" ou "PRO"
     * @param customerEmail email de l'OWNER (pour créer le customer Stripe)
     * @param newWorkspaceId ID du workspace nouvellement créé (PENDING_PAYMENT)
     * @return URL Stripe Checkout, ou {@link java.util.Optional#empty()} si
     *         Stripe est désactivé
     */
    public java.util.Optional<String> createSubscriptionSessionForNewWorkspace(
            String planCode, String customerEmail, java.util.UUID newWorkspaceId) {
        if (!stripeEnabled) {
            log.info("Stripe disabled — workspace {} sera activé immédiatement sans Checkout", newWorkspaceId);
            return java.util.Optional.empty();
        }
        if (!"TEAM".equals(planCode) && !"PRO".equals(planCode)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Plan invalide pour création workspace : " + planCode);
        }

        String priceId = resolvePriceId(planCode);
        if (priceId == null || priceId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Price ID non configuré pour le plan " + planCode);
        }

        // Création d'un customer Stripe dédié à ce nouveau workspace.
        String customerId = stripeCustomerService.createCustomer(customerEmail, newWorkspaceId).orElse(null);

        try {
            Stripe.apiKey = secretKey;
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    // SF-255-04 — active la saisie native d'un code promo
                    // STRIPE_DISCOUNT dans Stripe Checkout pour les
                    // workspaces nouvellement créés (PENDING_PAYMENT).
                    .setAllowPromotionCodes(true)
                    .setSuccessUrl(frontendUrl + "/workspaces?workspace_created=success&workspace_id=" + newWorkspaceId)
                    .setCancelUrl(frontendUrl + "/workspaces?workspace_created=cancelled&workspace_id=" + newWorkspaceId)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("plan_code", planCode)
                    .putMetadata("workspace_id", newWorkspaceId.toString())
                    .putMetadata("workspace_creation", "true");

            if (customerId != null) {
                params.setCustomer(customerId);
            } else {
                params.setCustomerEmail(customerEmail);
            }

            Session session = Session.create(params.build());
            return java.util.Optional.of(session.getUrl());
        } catch (StripeException e) {
            log.error("Failed to create Stripe checkout session for new workspace {}: {}",
                    newWorkspaceId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Service de paiement indisponible — création du workspace annulée");
        }
    }
}
