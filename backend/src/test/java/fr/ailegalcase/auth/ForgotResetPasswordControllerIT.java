package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "spring.security.oauth2.client.registration.microsoft.client-id=test-microsoft-id",
        "spring.security.oauth2.client.registration.microsoft.client-secret=test-microsoft-secret"
})
@AutoConfigureMockMvc
@Transactional
class ForgotResetPasswordControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String FORGOT_URL = "/api/v1/auth/forgot-password";
    private static final String RESET_URL = "/api/v1/auth/reset-password";

    private AuthAccount createLocalAccount(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Alice");
        user.setLastName("Dupont");
        user.setStatus("ACTIVE");
        userRepository.save(user);

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("LOCAL");
        account.setProviderUserId(email);
        account.setProviderEmail(email);
        account.setPasswordHash(passwordEncoder.encode("password123"));
        account.setEmailVerified(true);
        return authAccountRepository.save(account);
    }

    private PasswordResetToken createResetToken(String email, boolean expired, boolean used) {
        AuthAccount account = createLocalAccount(email);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(account.getUser());
        token.setToken("test-reset-token-" + email);
        token.setExpiresAt(expired ? Instant.now().minusSeconds(3600) : Instant.now().plusSeconds(86400));
        if (used) token.setUsedAt(Instant.now().minusSeconds(60));
        return passwordResetTokenRepository.save(token);
    }

    // I-01 : forgot-password email LOCAL → 200, token créé en base
    @Test
    void forgotPassword_localEmail_returns200AndCreatesToken() throws Exception {
        createLocalAccount("forgot@example.com");

        mockMvc.perform(post(FORGOT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"forgot@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").isNotEmpty());

        long tokenCount = passwordResetTokenRepository.findAll().stream()
                .filter(t -> t.getUser().getEmail().equals("forgot@example.com")).count();
        assertThat(tokenCount).isEqualTo(1);
    }

    // I-02 : forgot-password email inconnu → 200 (fail-silent), aucun token créé
    @Test
    void forgotPassword_unknownEmail_returns200NoToken() throws Exception {
        mockMvc.perform(post(FORGOT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@example.com"}
                                """))
                .andExpect(status().isOk());

        assertThat(passwordResetTokenRepository.count()).isEqualTo(0);
    }

    // I-03 : forgot-password email absent → 400
    @Test
    void forgotPassword_missingEmail_returns400() throws Exception {
        mockMvc.perform(post(FORGOT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // I-04 : reset-password token valide → 200, passwordHash mis à jour en base
    @Test
    void resetPassword_validToken_returns200AndUpdatesPassword() throws Exception {
        PasswordResetToken token = createResetToken("reset@example.com", false, false);

        mockMvc.perform(post(RESET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"newpassword123"}
                                """.formatted(token.getToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Mot de passe réinitialisé avec succès."));

        AuthAccount account = authAccountRepository
                .findByProviderAndProviderUserId("LOCAL", "reset@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("newpassword123", account.getPasswordHash())).isTrue();

        PasswordResetToken usedToken = passwordResetTokenRepository.findByToken(token.getToken()).orElseThrow();
        assertThat(usedToken.getUsedAt()).isNotNull();
    }

    // I-05 : reset-password token inconnu → 400
    @Test
    void resetPassword_unknownToken_returns400() throws Exception {
        mockMvc.perform(post(RESET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"nonexistent","newPassword":"newpassword123"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-06 : reset-password token expiré → 400
    @Test
    void resetPassword_expiredToken_returns400() throws Exception {
        PasswordResetToken token = createResetToken("expired@example.com", true, false);

        mockMvc.perform(post(RESET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"newpassword123"}
                                """.formatted(token.getToken())))
                .andExpect(status().isBadRequest());
    }

    // I-07 : reset-password token déjà utilisé → 400
    @Test
    void resetPassword_alreadyUsedToken_returns400() throws Exception {
        PasswordResetToken token = createResetToken("used@example.com", false, true);

        mockMvc.perform(post(RESET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"newpassword123"}
                                """.formatted(token.getToken())))
                .andExpect(status().isBadRequest());
    }

    // I-08 : reset-password newPassword trop court → 400
    @Test
    void resetPassword_shortPassword_returns400() throws Exception {
        mockMvc.perform(post(RESET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"any-token","newPassword":"short"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-09 : les deux endpoints accessibles sans auth (pas de 401/403)
    @Test
    void bothEndpoints_arePublic() throws Exception {
        // forgot-password sur email inconnu → 200 (pas 401)
        mockMvc.perform(post(FORGOT_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pub@example.com"}
                                """))
                .andExpect(status().isOk());

        // reset-password avec token inconnu → 400 (pas 401/403)
        mockMvc.perform(post(RESET_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"any","newPassword":"password123"}
                                """))
                .andExpect(status().isBadRequest());
    }
}
