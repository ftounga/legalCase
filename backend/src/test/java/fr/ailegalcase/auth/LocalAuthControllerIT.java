package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
})
@AutoConfigureMockMvc
@Transactional
class LocalAuthControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String VERIFY_URL = "/api/v1/auth/verify-email";

    private String registerJson(String firstName, String lastName, String email, String password) {
        return """
                {"firstName":"%s","lastName":"%s","email":"%s","password":"%s"}
                """.formatted(firstName, lastName, email, password);
    }

    // I-01 : inscription nominale → 201, User + AuthAccount LOCAL créés en base
    @Test
    void register_nominal_returns201AndPersistsEntities() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Alice", "Dupont", "alice@example.com", "password123")))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", "alice@example.com").orElseThrow();
        assertThat(account.isEmailVerified()).isFalse();
        assertThat(account.getPasswordHash()).isNotBlank();
    }

    // I-02 : email normalisé → recherché en minuscules
    @Test
    void register_emailUppercase_normalizedToLowercase() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Bob", "Martin", "BOB@EXAMPLE.COM", "password123")))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
    }

    // I-03 : email déjà utilisé → 409
    @Test
    void register_duplicateEmail_returns409() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Alice", "Dupont", "dup@example.com", "password123")))
                .andExpect(status().isCreated());

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Alicia", "Martin", "dup@example.com", "otherpass")))
                .andExpect(status().isConflict());
    }

    // I-04 : champ manquant → 400
    @Test
    void register_missingFirstName_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"lastName":"Dupont","email":"x@x.com","password":"password123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-05 : format email invalide → 400
    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Alice", "Dupont", "not-an-email", "password123")))
                .andExpect(status().isBadRequest());
    }

    // I-06 : password trop court → 400
    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Alice", "Dupont", "alice2@example.com", "short")))
                .andExpect(status().isBadRequest());
    }

    // I-07 : endpoint /register accessible sans auth
    @Test
    void register_noAuth_isPublic() throws Exception {
        // endpoint ne demande pas d'auth — vérification via 201 (pas 401/403)
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Public", "User", "pub@example.com", "password123")))
                .andExpect(status().isCreated());
    }

    // I-08 : verify-email token valide → 200, emailVerified = true en base
    @Test
    void verifyEmail_validToken_returns200AndSetsEmailVerified() throws Exception {
        // Inscription d'abord
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson("Claire", "Lebrun", "claire@example.com", "password123")))
                .andExpect(status().isCreated());

        String token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getEmail().equals("claire@example.com"))
                .findFirst()
                .orElseThrow()
                .getToken();

        mockMvc.perform(get(VERIFY_URL).param("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Email validé avec succès."));

        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", "claire@example.com").orElseThrow();
        assertThat(account.isEmailVerified()).isTrue();
    }

    // I-09 : verify-email token inconnu → 400
    @Test
    void verifyEmail_unknownToken_returns400() throws Exception {
        mockMvc.perform(get(VERIFY_URL).param("token", "inexistant-token"))
                .andExpect(status().isBadRequest());
    }

    // I-10 : verify-email token expiré → 400
    @Test
    void verifyEmail_expiredToken_returns400() throws Exception {
        User user = new User();
        user.setEmail("expired@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken("expired-tok-it");
        token.setExpiresAt(Instant.now().minusSeconds(3600));
        emailVerificationTokenRepository.save(token);

        mockMvc.perform(get(VERIFY_URL).param("token", "expired-tok-it"))
                .andExpect(status().isBadRequest());
    }

    // I-11 : verify-email token déjà utilisé → 400
    @Test
    void verifyEmail_alreadyUsedToken_returns400() throws Exception {
        User user = new User();
        user.setEmail("used@example.com");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken("used-tok-it");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setUsedAt(Instant.now().minusSeconds(60));
        emailVerificationTokenRepository.save(token);

        mockMvc.perform(get(VERIFY_URL).param("token", "used-tok-it"))
                .andExpect(status().isBadRequest());
    }

    // I-12 : endpoint /verify-email accessible sans auth
    @Test
    void verifyEmail_noAuth_isPublic() throws Exception {
        // Pas de 401 sur un token inconnu — la réponse est 400 (token inconnu), pas 401
        mockMvc.perform(get(VERIFY_URL).param("token", "any-token"))
                .andExpect(status().isBadRequest());
    }
}
