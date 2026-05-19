package fr.ailegalcase.billing;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * SF-247-01 : endpoints de résiliation d'abonnement en self-service.
 *
 * <ul>
 *   <li>{@code POST /api/v1/billing/cancel} — programme la résiliation (OWNER)</li>
 *   <li>{@code POST /api/v1/billing/resume} — annule une résiliation programmée (OWNER)</li>
 *   <li>{@code GET /api/v1/billing/subscription} — état courant (tout membre)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/billing")
public class BillingCancellationController {

    private final BillingCancellationService billingCancellationService;

    public BillingCancellationController(BillingCancellationService billingCancellationService) {
        this.billingCancellationService = billingCancellationService;
    }

    @PostMapping("/cancel")
    public SubscriptionStateResponse cancel(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return billingCancellationService.cancel(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @PostMapping("/resume")
    public SubscriptionStateResponse resume(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return billingCancellationService.resume(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    @GetMapping("/subscription")
    public SubscriptionStateResponse getSubscription(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return billingCancellationService.getState(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
