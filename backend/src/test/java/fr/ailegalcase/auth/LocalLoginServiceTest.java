package fr.ailegalcase.auth;

import fr.ailegalcase.workspace.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalLoginServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpServletResponse httpResponse;
    @Mock private HttpSession httpSession;

    private LocalAuthService service;

    @BeforeEach
    void setUp() {
        service = new LocalAuthService(userRepository, authAccountRepository,
                emailVerificationTokenRepository, passwordEncoder, emailService);
    }

    private AuthAccount localAccount(boolean emailVerified, String passwordHash) {
        User user = new User();
        user.setEmail("alice@example.com");
        user.setFirstName("Alice");
        user.setLastName("Dupont");
        user.setStatus("ACTIVE");

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("LOCAL");
        account.setProviderUserId("alice@example.com");
        account.setPasswordHash(passwordHash);
        account.setEmailVerified(emailVerified);
        return account;
    }

    // U-01 : login nominal → MeResponse retourné, session créée
    @Test
    void login_nominal_returnsMeResponse() {
        when(httpRequest.getSession(any(Boolean.class))).thenReturn(httpSession);
        AuthAccount account = localAccount(true, "$2a$10$hash");
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "alice@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);

        MeResponse response = service.login(
                new LocalLoginRequest("alice@example.com", "password123"), httpRequest, httpResponse);

        assertThat(response.email()).isEqualTo("alice@example.com");
        assertThat(response.provider()).isEqualTo("LOCAL");
    }

    // U-02 : email inconnu → 401
    @Test
    void login_unknownEmail_throws401() {
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(
                new LocalLoginRequest("unknown@example.com", "password123"), httpRequest, httpResponse))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // U-03 : mauvais mot de passe → 401 (même message que email inconnu)
    @Test
    void login_wrongPassword_throws401() {
        AuthAccount account = localAccount(true, "$2a$10$hash");
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "alice@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong", "$2a$10$hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(
                new LocalLoginRequest("alice@example.com", "wrong"), httpRequest, httpResponse))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.UNAUTHORIZED));
    }

    // U-04 : emailVerified = false → 403
    @Test
    void login_emailNotVerified_throws403() {
        AuthAccount account = localAccount(false, "$2a$10$hash");
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "alice@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "$2a$10$hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(
                new LocalLoginRequest("alice@example.com", "password123"), httpRequest, httpResponse))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
