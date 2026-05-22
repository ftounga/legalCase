package fr.ailegalcase.billing;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-255 SF-255-01 — endpoint utilisateur de redemption d'un code promo
 * TRIAL_EXTENSION.
 *
 * <p>Le {@code workspaceId} est obligatoirement lu du path : le body ne
 * contient que le code à redeem. La membership est revérifiée côté service
 * (isolation workspace stricte).
 */
@RestController
@RequestMapping("/api/v1/workspaces/{workspaceId}/billing/promo-codes")
public class PromoCodeRedemptionController {

    private final PromoCodeService promoCodeService;

    public PromoCodeRedemptionController(PromoCodeService promoCodeService) {
        this.promoCodeService = promoCodeService;
    }

    @PostMapping("/redeem")
    public RedeemResponse redeem(@PathVariable UUID workspaceId,
                                 @Valid @RequestBody RedeemRequest request,
                                 @AuthenticationPrincipal OidcUser oidcUser,
                                 Principal principal) {
        return promoCodeService.redeemTrialExtension(workspaceId, request, oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }
}
