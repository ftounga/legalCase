package fr.ailegalcase.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class MeController {

    private static final Logger log = LoggerFactory.getLogger(MeController.class);

    private final AuthAccountRepository authAccountRepository;

    public MeController(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public MeResponse me(@AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        String provider = resolveProvider(principal);

        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("account_not_found"), "Auth account not found"));

        User user = account.getUser();
        return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), account.getProvider());
    }

    // Kept in controller — single-line logic not worth a dedicated service in V1
    private String resolveProvider(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId().toUpperCase();
        }
        log.warn("Unexpected principal type in /api/me: {}", principal != null ? principal.getClass().getName() : "null");
        return "UNKNOWN";
    }
}
