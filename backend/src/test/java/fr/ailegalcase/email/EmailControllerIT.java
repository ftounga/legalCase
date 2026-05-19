package fr.ailegalcase.email;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * F-248 SF-248-01 — Tests d'intégration de {@link EmailController}.
 *
 * <p>Endpoints publics {@code /api/v1/public/email/**} : aucune authentification,
 * le token de désinscription fait foi.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
class EmailControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;

    private static final String UNSUBSCRIBE = "/api/v1/public/email/unsubscribe";
    private static final String RESUBSCRIBE = "/api/v1/public/email/resubscribe";
    private static final String STATUS = "/api/v1/public/email/subscription-status";

    private User seedUserWithToken(UUID token) {
        User user = new User();
        user.setEmail("it-" + UUID.randomUUID() + "@example.com");
        user.setStatus("ACTIVE");
        user.setMarketingUnsubscribeToken(token);
        return userRepository.save(user);
    }

    // I-01 : unsubscribe avec token valide → 200 {optedOut:true}, sans authentification
    @Test
    void unsubscribe_validToken_returns200OptedOutTrue() throws Exception {
        UUID token = UUID.randomUUID();
        User user = seedUserWithToken(token);

        mockMvc.perform(post(UNSUBSCRIBE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedOut").value(true));

        assertThat(userRepository.findById(user.getId()).orElseThrow().isMarketingEmailsOptedOut()).isTrue();
    }

    // I-02a : unsubscribe token absent → 400
    @Test
    void unsubscribe_missingToken_returns400() throws Exception {
        mockMvc.perform(post(UNSUBSCRIBE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // I-02b : unsubscribe token mal formé → 400
    @Test
    void unsubscribe_malformedToken_returns400() throws Exception {
        mockMvc.perform(post(UNSUBSCRIBE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"pas-un-uuid\"}"))
                .andExpect(status().isBadRequest());
    }

    // I-02c : unsubscribe token inconnu en base → 404
    @Test
    void unsubscribe_unknownToken_returns404() throws Exception {
        mockMvc.perform(post(UNSUBSCRIBE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound());
    }

    // I-03 : resubscribe → 200 {optedOut:false}
    @Test
    void resubscribe_validToken_returns200OptedOutFalse() throws Exception {
        UUID token = UUID.randomUUID();
        User user = seedUserWithToken(token);
        user.setMarketingEmailsOptedOut(true);
        userRepository.save(user);

        mockMvc.perform(post(RESUBSCRIBE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedOut").value(false));

        assertThat(userRepository.findById(user.getId()).orElseThrow().isMarketingEmailsOptedOut()).isFalse();
    }

    // I-04 : subscription-status → 200 {optedOut}
    @Test
    void subscriptionStatus_validToken_returnsCurrentState() throws Exception {
        UUID token = UUID.randomUUID();
        seedUserWithToken(token);

        mockMvc.perform(get(STATUS).param("token", token.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedOut").value(false));
    }

    // I-05 : subscription-status sans token → 400
    @Test
    void subscriptionStatus_missingToken_returns400() throws Exception {
        mockMvc.perform(get(STATUS))
                .andExpect(status().isBadRequest());
    }

    // I-06 : unsubscribe idempotent — deux appels successifs → toujours 200 {optedOut:true}
    @Test
    void unsubscribe_calledTwice_isIdempotent() throws Exception {
        UUID token = UUID.randomUUID();
        seedUserWithToken(token);
        String body = "{\"token\":\"" + token + "\"}";

        mockMvc.perform(post(UNSUBSCRIBE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedOut").value(true));
        mockMvc.perform(post(UNSUBSCRIBE).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.optedOut").value(true));
    }

    // I-07 : endpoint accessible sans token JWT (public)
    @Test
    void unsubscribe_noAuth_isPermitted() throws Exception {
        UUID token = UUID.randomUUID();
        seedUserWithToken(token);

        mockMvc.perform(post(UNSUBSCRIBE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk());
    }
}
