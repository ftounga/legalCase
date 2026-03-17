package fr.ailegalcase.auth;

import fr.ailegalcase.shared.OAuthProviderResolver;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final AuthAccountRepository authAccountRepository;

    public MeController(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public MeResponse me(@AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);

        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("account_not_found"), "Auth account not found"));

        User user = account.getUser();
        return new MeResponse(user.getId(), user.getEmail(), user.getFirstName(),
                user.getLastName(), account.getProvider());
    }

}
