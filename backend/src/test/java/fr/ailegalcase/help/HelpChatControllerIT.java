package fr.ailegalcase.help;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class HelpChatControllerIT {

    @Autowired private MockMvc mockMvc;
    @MockBean private HelpChatService helpChatService;

    private static final String URL = "/api/v1/help/chat";

    // IT-01 : POST sans auth → 401
    @Test
    void chat_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Comment créer un dossier ?"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    // IT-02 : POST message valide → 200 + champ answer présent
    @Test
    void chat_validMessage_returns200WithAnswer() throws Exception {
        when(helpChatService.chat(anyString()))
                .thenReturn(new HelpChatResponse("Pour créer un dossier, cliquez sur Nouveau dossier."));

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Comment créer un dossier ?"}
                                """)
                        .with(authentication(buildGoogleAuth("google-help-sub", "help-user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").isString())
                .andExpect(jsonPath("$.answer").isNotEmpty());
    }

    // IT-03 : POST message vide → 400
    @Test
    void chat_emptyMessage_returns400() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": ""}
                                """)
                        .with(authentication(buildGoogleAuth("google-help-empty-sub", "help-empty@example.com"))))
                .andExpect(status().isBadRequest());
    }

    // IT-04 : POST message > 500 chars → 400
    @Test
    void chat_messageTooLong_returns400() throws Exception {
        String longMessage = "a".repeat(501);
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\": \"" + longMessage + "\"}")
                        .with(authentication(buildGoogleAuth("google-help-long-sub", "help-long@example.com"))))
                .andExpect(status().isBadRequest());
    }

    private OAuth2AuthenticationToken buildGoogleAuth(String sub, String email) {
        Map<String, Object> claims = Map.of(
                "sub", sub,
                "email", email,
                "iss", "https://accounts.google.com"
        );
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
