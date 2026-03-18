package fr.ailegalcase.billing;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/stripe")
public class StripeCheckoutController {

    private final StripeCheckoutService checkoutService;

    public StripeCheckoutController(StripeCheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/checkout-session")
    public ResponseEntity<Map<String, String>> createCheckoutSession(
            @Valid @RequestBody CheckoutSessionRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {

        String checkoutUrl = checkoutService.createCheckoutSession(
                request.planCode(), oidcUser, OAuthProviderResolver.resolve(principal));
        return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));
    }

    public record CheckoutSessionRequest(@NotBlank String planCode) {}
}
