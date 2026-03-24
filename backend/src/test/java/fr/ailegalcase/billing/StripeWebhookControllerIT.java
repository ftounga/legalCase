package fr.ailegalcase.billing;

import com.stripe.net.Webhook;
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
        "app.stripe.webhook-secret=whsec_test_secret"
})
@AutoConfigureMockMvc
class StripeWebhookControllerIT {

    @Autowired private MockMvc mockMvc;
    @MockBean private StripeWebhookService webhookService;

    // I-01 : endpoint accessible sans authentification
    @Test
    void webhook_noAuth_notRejectedWith401() throws Exception {
        mockMvc.perform(post("/api/v1/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "invalid_sig")
                        .content("{}"))
                .andExpect(status().isBadRequest()); // 400 signature invalide, pas 401
    }

    // I-02 : signature invalide → 400
    @Test
    void webhook_invalidSignature_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=123,v1=invalidsig")
                        .content("{\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isBadRequest());
    }
}
