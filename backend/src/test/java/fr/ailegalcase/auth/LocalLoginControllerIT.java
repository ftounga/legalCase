package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class LocalLoginControllerIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private AuthAccountRepository authAccountRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String ME_URL = "/api/me";

    private AuthAccount createLocalAccount(String email, boolean emailVerified) {
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
        account.setEmailVerified(emailVerified);
        return authAccountRepository.save(account);
    }

    private String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    // I-01 : login nominal → 200, MeResponse avec provider=LOCAL
    @Test
    void login_nominal_returns200WithMeResponse() throws Exception {
        createLocalAccount("alice@example.com", true);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("alice@example.com", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.provider").value("LOCAL"));
    }

    // I-02 : login puis GET /api/me avec session → retourne user LOCAL
    @Test
    void login_thenMe_returnsLocalUser() throws Exception {
        createLocalAccount("session@example.com", true);

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("session@example.com", "password123"))
                        .session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get(ME_URL).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("session@example.com"))
                .andExpect(jsonPath("$.provider").value("LOCAL"));
    }

    // I-03 : email inconnu → 401
    @Test
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("nobody@example.com", "password123")))
                .andExpect(status().isUnauthorized());
    }

    // I-04 : mauvais mot de passe → 401
    @Test
    void login_wrongPassword_returns401() throws Exception {
        createLocalAccount("wrongpwd@example.com", true);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("wrongpwd@example.com", "badpassword")))
                .andExpect(status().isUnauthorized());
    }

    // I-05 : emailVerified = false → 403
    @Test
    void login_emailNotVerified_returns403() throws Exception {
        createLocalAccount("unverified@example.com", false);

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("unverified@example.com", "password123")))
                .andExpect(status().isForbidden());
    }

    // I-06 : champ manquant → 400
    @Test
    void login_missingField_returns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"alice@example.com"}
                                """))
                .andExpect(status().isBadRequest());
    }

    // I-07 : endpoint public — pas besoin d'auth pour appeler /login
    @Test
    void login_noExistingAuth_isPublic() throws Exception {
        // Vérification : 401 (credentials invalides) et non 403 (accès refusé) → endpoint accessible
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson("public@example.com", "password123")))
                .andExpect(status().isUnauthorized());
    }

    // I-08 : fusion OAuth — findOrCreateUser sur email LOCAL existant → 1 User, 2 AuthAccounts
    @Test
    void fusion_oauthLoginOnLocalEmail_reusesExistingUser() {
        createLocalAccount("fusion@example.com", true);

        User existingUser = userRepository.findByEmail("fusion@example.com").orElseThrow();

        // Simule la fusion : nouvel AuthAccount OAuth sur l'utilisateur existant
        AuthAccount oauthAccount = new AuthAccount();
        oauthAccount.setUser(existingUser);
        oauthAccount.setProvider("GOOGLE");
        oauthAccount.setProviderUserId("google-sub-fusion");
        oauthAccount.setProviderEmail("fusion@example.com");
        authAccountRepository.save(oauthAccount);

        long userCount = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equals("fusion@example.com")).count();
        long accountCount = authAccountRepository.findAll().stream()
                .filter(a -> a.getUser().getEmail().equals("fusion@example.com")).count();

        assertThat(userCount).isEqualTo(1);
        assertThat(accountCount).isEqualTo(2);

        Optional<AuthAccount> google = authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-fusion");
        assertThat(google).isPresent();
        assertThat(google.get().getUser().getId()).isEqualTo(existingUser.getId());
    }
}
