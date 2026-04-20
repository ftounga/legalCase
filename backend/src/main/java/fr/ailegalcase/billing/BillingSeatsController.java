package fr.ailegalcase.billing;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingSeatsController {

    private final BillingSeatsService billingSeatsService;

    public BillingSeatsController(BillingSeatsService billingSeatsService) {
        this.billingSeatsService = billingSeatsService;
    }

    @GetMapping("/seats-summary")
    public SeatsSummaryResponse getSeatsSummary(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return billingSeatsService.getSummary(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
