package fr.ailegalcase.shared;

import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

import java.security.Principal;

public class OAuthProviderResolver {

    private OAuthProviderResolver() {}

    public static String resolve(Principal principal) {
        if (principal instanceof OAuth2AuthenticationToken token) {
            return token.getAuthorizedClientRegistrationId().toUpperCase();
        }
        return "UNKNOWN";
    }
}
