package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "app.frontend-url=http://localhost:4200"
})
@AutoConfigureMockMvc
class FrontendRedirectIT {

    @Autowired
    private MockMvc mockMvc;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // I-01 : app.frontend-url est injecté correctement dans le contexte Spring
    @Test
    void frontendUrl_property_isInjected() {
        assertThat(frontendUrl).isEqualTo("http://localhost:4200");
    }

    // I-02 : le contexte Spring démarre avec app.frontend-url — SecurityConfig charge sans erreur
    // (si @Value échoue, le contexte ne démarre pas et tous les tests échouent)
    @Test
    void securityConfig_withFrontendUrl_startsCorrectly() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", containsString("accounts.google.com")));
    }
}
