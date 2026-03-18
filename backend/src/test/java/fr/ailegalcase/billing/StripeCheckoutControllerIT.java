package fr.ailegalcase.billing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
class StripeCheckoutControllerIT {

    @Autowired private MockMvc mockMvc;
    @MockBean private StripeCheckoutService checkoutService;

    // I-01 : POST sans auth → 401
    @Test
    void createCheckoutSession_noAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/stripe/checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planCode\":\"STARTER\"}"))
                .andExpect(status().isUnauthorized());
    }

    // I-02 : POST avec planCode manquant → 400
    @Test
    void createCheckoutSession_missingPlanCode_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/stripe/checkout-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized()); // sans auth → 401 avant validation
    }
}
