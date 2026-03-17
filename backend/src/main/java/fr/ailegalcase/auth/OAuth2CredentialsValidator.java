package fr.ailegalcase.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class OAuth2CredentialsValidator {

    private static final Logger log = LoggerFactory.getLogger(OAuth2CredentialsValidator.class);
    private static final String PLACEHOLDER = "change-me";

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.microsoft.client-id}")
    private String microsoftClientId;

    @EventListener(ApplicationReadyEvent.class)
    public void warnIfCredentialsAreDefault() {
        if (PLACEHOLDER.equals(googleClientId)) {
            log.warn("OAuth2 Google credentials not configured — set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET");
        }
        if (PLACEHOLDER.equals(microsoftClientId)) {
            log.warn("OAuth2 Microsoft credentials not configured — set MICROSOFT_CLIENT_ID and MICROSOFT_CLIENT_SECRET");
        }
    }
}
