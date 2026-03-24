package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class SecurityConfigIT {

    @Autowired
    private MockMvc mockMvc;

    // I-01 + I-04 : GET /api/hello sans auth → 401 JSON (pas de HTML)
    @Test
    void apiEndpoint_withoutAuth_returns401Json() throws Exception {
        mockMvc.perform(get("/api/hello").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(content().string(not(containsString("<html"))));
    }

    // I-07 : le corps de la réponse 401 ne contient pas de stacktrace
    @Test
    void unauthorizedResponse_doesNotContainStacktrace() throws Exception {
        mockMvc.perform(get("/api/hello"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(not(containsString("at fr."))));
    }

    // I-02 : /oauth2/authorization/google → 302 vers Google
    @Test
    void oauthGoogle_withoutAuth_redirectsToGoogle() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }

    // I-06 : endpoint /api/** inconnu sans auth → 401 (l'auth est vérifiée avant le routing)
    @Test
    void unknownApiEndpoint_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/nonexistent"))
                .andExpect(status().isUnauthorized());
    }

    // I-05 : démarrage application avec config OAuth valide → contexte chargé (couvert par tous les tests ci-dessus)
}
