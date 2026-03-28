package fr.ailegalcase.billing;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
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
public class TopupCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(TopupCheckoutService.class);
    private static final Set<String> ALLOWED_ROLES = Set.of("OWNER", "ADMIN");

    private final boolean stripeEnabled;
    private final String secretKey;
    private final String priceIdTokens1m;
    private final String priceIdTokens5m;
    private final String priceIdTokens20m;
    private final String frontendUrl;
    private final SubscriptionRepository subscriptionRepository;
    private final StripeCustomerService stripeCustomerService;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public TopupCheckoutService(
            @Value("${app.stripe.enabled:false}") boolean stripeEnabled,
            @Value("${app.stripe.secret-key:}") String secretKey,
            @Value("${app.stripe.price-id-tokens-1m:}") String priceIdTokens1m,
            @Value("${app.stripe.price-id-tokens-5m:}") String priceIdTokens5m,
            @Value("${app.stripe.price-id-tokens-20m:}") String priceIdTokens20m,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            SubscriptionRepository subscriptionRepository,
            StripeCustomerService stripeCustomerService,
            CurrentUserResolver currentUserResolver,
            WorkspaceMemberRepository workspaceMemberRepository) {
        this.stripeEnabled = stripeEnabled;
        this.secretKey = secretKey;
        this.priceIdTokens1m = priceIdTokens1m;
        this.priceIdTokens5m = priceIdTokens5m;
        this.priceIdTokens20m = priceIdTokens20m;
        this.frontendUrl = frontendUrl;
        this.subscriptionRepository = subscriptionRepository;
        this.stripeCustomerService = stripeCustomerService;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @Transactional
    public String createTopupSession(String packCode, OidcUser oidcUser, String provider, Principal principal) {
        if (!stripeEnabled) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Stripe is not enabled");
        }

        TokenPack pack;
        try {
            pack = TokenPack.valueOf(packCode);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid pack code: " + packCode);
        }

        User user = currentUserResolver.resolve(oidcUser, provider, principal);

        var member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));

        if (!ALLOWED_ROLES.contains(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only OWNER or ADMIN can purchase tokens");
        }

        var workspace = member.getWorkspace();

        fr.ailegalcase.billing.Subscription sub = subscriptionRepository
                .findByWorkspaceId(workspace.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Subscription not found"));

        if (sub.getStripeCustomerId() == null) {
            stripeCustomerService.createCustomer(user.getEmail(), workspace.getId())
                    .ifPresent(customerId -> {
                        sub.setStripeCustomerId(customerId);
                        subscriptionRepository.save(sub);
                    });
        }

        String priceId = switch (pack) {
            case TOKENS_20M -> priceIdTokens20m;
            case TOKENS_5M  -> priceIdTokens5m;
            case TOKENS_1M  -> priceIdTokens1m;
        };

        try {
            Stripe.apiKey = secretKey;
            SessionCreateParams.Builder params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(frontendUrl + "/workspace/billing?topup=success")
                    .setCancelUrl(frontendUrl + "/workspace/billing?topup=canceled")
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .putMetadata("pack_code", pack.name())
                    .putMetadata("workspace_id", workspace.getId().toString());

            if (sub.getStripeCustomerId() != null) {
                params.setCustomer(sub.getStripeCustomerId());
            }

            Session session = Session.create(params.build());
            return session.getUrl();
        } catch (StripeException e) {
            log.error("Failed to create topup session for workspace {}: {}", workspace.getId(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Payment service unavailable");
        }
    }
}
