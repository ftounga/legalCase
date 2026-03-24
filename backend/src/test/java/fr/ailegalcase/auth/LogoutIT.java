package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class LogoutIT {

    @Autowired
    private MockMvc mockMvc;

    // I-01 : POST /api/logout avec session active → 200 JSON
    @Test
    void logout_withActiveSession_returns200Json() throws Exception {
        OAuth2AuthenticationToken auth = buildGoogleAuth();

        mockMvc.perform(post("/api/logout")
                        .accept(MediaType.APPLICATION_JSON)
                        .with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Logged out successfully"))
                .andExpect(content().string(not(containsString("<html"))));
    }

    // I-02 : GET /api/me après logout → 401
    @Test
    void me_afterLogout_returns401() throws Exception {
        OAuth2AuthenticationToken auth = buildGoogleAuth();
        MockHttpSession session = new MockHttpSession();

        // Login — établir une session
        mockMvc.perform(get("/api/me")
                        .session(session)
                        .with(authentication(auth)));

        // Logout — invalider la session
        mockMvc.perform(post("/api/logout")
                .session(session));

        // Après logout, la même session ne doit plus authentifier
        mockMvc.perform(get("/api/me")
                        .session(session)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // I-03 : POST /api/logout sans session → 200 (idempotent)
    @Test
    void logout_withoutSession_returns200() throws Exception {
        mockMvc.perform(post("/api/logout")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    private OAuth2AuthenticationToken buildGoogleAuth() {
        Map<String, Object> claims = Map.of(
                "sub", "google-sub-123",
                "email", "john@example.com",
                "iss", "https://accounts.google.com"
        );
        OidcIdToken idToken = new OidcIdToken("token-value", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(
                List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        return new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");
    }
}
