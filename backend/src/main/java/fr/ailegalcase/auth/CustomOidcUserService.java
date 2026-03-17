package fr.ailegalcase.auth;

import fr.ailegalcase.workspace.WorkspaceService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;
    private final AuthAccountRepository authAccountRepository;
    private final WorkspaceService workspaceService;

    public CustomOidcUserService(UserRepository userRepository,
                                 AuthAccountRepository authAccountRepository,
                                 WorkspaceService workspaceService) {
        this.userRepository = userRepository;
        this.authAccountRepository = authAccountRepository;
        this.workspaceService = workspaceService;
    }

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Set<String> scopes = userRequest.getAccessToken().getScopes();
        return findOrCreateUser(oidcUser, provider, scopes);
    }

    // package-private for testing
    OidcUser findOrCreateUser(OidcUser oidcUser, String provider, Set<String> scopes) {
        String providerUserId = oidcUser.getSubject();
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_sub"),
                    "OIDC claim 'sub' is required");
        }

        String email = oidcUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_email"),
                    "OIDC claim 'email' is required");
        }

        authAccountRepository.findByProviderAndProviderUserId(provider, providerUserId)
                .orElseGet(() -> createUser(oidcUser, provider, providerUserId, scopes));

        return oidcUser;
    }

    private AuthAccount createUser(OidcUser oidcUser, String provider,
                                   String providerUserId, Set<String> scopes) {
        User user = new User();
        user.setEmail(oidcUser.getEmail());
        user.setFirstName(oidcUser.getGivenName());
        user.setLastName(oidcUser.getFamilyName());
        user.setStatus("ACTIVE");
        userRepository.save(user);
        workspaceService.createDefaultWorkspace(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider(provider);
        account.setProviderUserId(providerUserId);
        account.setProviderEmail(oidcUser.getEmail());
        account.setAccessScope(scopes != null ? String.join(" ", scopes) : null);
        return authAccountRepository.save(account);
    }
}
