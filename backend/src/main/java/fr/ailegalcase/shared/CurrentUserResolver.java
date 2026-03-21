package fr.ailegalcase.shared;

import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;

/**
 * Résout l'utilisateur courant quel que soit le type d'authentification :
 * - OAuth2/OIDC (Google, Microsoft) : via OidcUser.getSubject() + provider
 * - Auth locale (email/mdp) : via principal.getName() = email, provider = "LOCAL"
 */
@Service
public class CurrentUserResolver {

    private final AuthAccountRepository authAccountRepository;

    public CurrentUserResolver(AuthAccountRepository authAccountRepository) {
        this.authAccountRepository = authAccountRepository;
    }

    public User resolve(OidcUser oidcUser, String provider, Principal principal) {
        if (oidcUser != null) {
            return authAccountRepository
                    .findByProviderAndProviderUserId(provider, oidcUser.getSubject())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                    .getUser();
        }
        // Auth locale : principal.getName() = email
        return authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Session invalide"))
                .getUser();
    }
}
