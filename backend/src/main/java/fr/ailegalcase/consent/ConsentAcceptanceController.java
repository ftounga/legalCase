package fr.ailegalcase.consent;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.consent.dto.ConsentAcceptanceRequest;
import fr.ailegalcase.consent.dto.ConsentAcceptanceResponse;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * F-240 SF-240-01 : endpoint d'enregistrement des acceptations contractuelles.
 * Consommé par SF-240-02 (sign-up), SF-240-03 (paiement) et SF-240-04 (DPA).
 */
@RestController
@RequestMapping("/api/v1/consent")
public class ConsentAcceptanceController {

    private final ConsentAcceptanceService consentAcceptanceService;
    private final CurrentUserResolver currentUserResolver;

    public ConsentAcceptanceController(ConsentAcceptanceService consentAcceptanceService,
                                       CurrentUserResolver currentUserResolver) {
        this.consentAcceptanceService = consentAcceptanceService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/accept")
    @ResponseStatus(HttpStatus.CREATED)
    public ConsentAcceptanceResponse accept(
            @RequestBody ConsentAcceptanceRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            HttpServletRequest httpRequest) {
        String provider = OAuthProviderResolver.resolve(principal);
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        return consentAcceptanceService.accept(request, user, httpRequest);
    }
}
