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
    private static final Set<String> VALID_PLANS = Set.of("STARTER", "PRO");

    private final boolean stripeEnabled;
    private final String secretKey;
    private final String priceIdStarter;
    private final String priceIdPro;
    private final String frontendUrl;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeCustomerService stripeCustomerService;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public StripeCheckoutService(
            @Value("${app.stripe.enabled:false}") boolean stripeEnabled,
            @Value("${app.stripe.secret-key:}") String secretKey,
            @Value("${app.stripe.price-id-starter:}") String priceIdStarter,
            @Value("${app.stripe.price-id-pro:}") String priceIdPro,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            SubscriptionRepository subscriptionRepository,
            StripeCustomerService stripeCustomerService,
            CurrentUserResolver currentUserResolver,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.stripeEnabled = stripeEnabled;
        this.secretKey = secretKey;
        this.priceIdStarter = priceIdStarter;
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

        String priceId = "PRO".equals(planCode) ? priceIdPro : priceIdStarter;

        try {
            Stripe.apiKey = secretKey;
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
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
}
