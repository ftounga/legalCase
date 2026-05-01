package fr.ailegalcase.shared.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "app.frontend-url=https://legalcase.fr",
        "app.allowed-frontend-urls=https://legalcase.fr,https://legalcase.ng-itconsulting.com"
})
@AutoConfigureMockMvc
class WebConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void cors_allows_primary_frontend_url() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header("Origin", "https://legalcase.fr")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://legalcase.fr"));
    }

    @Test
    void cors_allows_legacy_frontend_url() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header("Origin", "https://legalcase.ng-itconsulting.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://legalcase.ng-itconsulting.com"));
    }

    @Test
    void cors_allows_local_dev_origin() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }

    @Test
    void cors_blocks_disallowed_origin() throws Exception {
        mockMvc.perform(options("/api/v1/me")
                        .header("Origin", "https://malicious.example.com")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
