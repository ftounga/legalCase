package fr.ailegalcase.auth;

import fr.ailegalcase.workspace.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private LocalAuthService service;

    @BeforeEach
    void setUp() {
        service = new LocalAuthService(userRepository, authAccountRepository,
                emailVerificationTokenRepository, passwordEncoder, emailService);
    }

    private RegisterRequest validRequest() {
        return new RegisterRequest("Alice", "Dupont", "alice@example.com", "password123");
    }

    private User savedUser() {
        User u = new User();
        u.setEmail("alice@example.com");
        u.setStatus("ACTIVE");
        return u;
    }

    // U-01 : inscription nominale — User + AuthAccount + Token créés, email envoyé
    @Test
    void register_nominal_createsUserAccountAndToken() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authAccountRepository.save(any(AuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationTokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hash");

        service.register(validRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<AuthAccount> accountCaptor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getProvider()).isEqualTo("LOCAL");
        assertThat(accountCaptor.getValue().getProviderUserId()).isEqualTo("alice@example.com");
        assertThat(accountCaptor.getValue().getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(accountCaptor.getValue().isEmailVerified()).isFalse();

        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailService).sendEmailVerification(eq("alice@example.com"), anyString());
    }

    // U-02 : email normalisé en minuscules
    @Test
    void register_emailNormalized_toLowerCase() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(authAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hash");

        service.register(new RegisterRequest("Alice", "Dupont", "ALICE@EXAMPLE.COM", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("alice@example.com");
    }

    // U-03 : email déjà utilisé → 409
    @Test
    void register_emailAlreadyUsed_throws409() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(validRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(userRepository, never()).save(any());
        verify(authAccountRepository, never()).save(any());
    }

    // U-04 : fail-open email — inscription réussit si envoi échoue
    @Test
    void register_emailSendFails_registrationSucceeds() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(authAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        doThrow(new MailSendException("SMTP error")).when(emailService).sendEmailVerification(any(), any());

        assertThatNoException().isThrownBy(() -> service.register(validRequest()));

        verify(userRepository).save(any());
        verify(authAccountRepository).save(any());
        verify(emailVerificationTokenRepository).save(any());
    }

    // U-05 : verifyEmail — token valide → emailVerified = true, usedAt renseigné
    @Test
    void verifyEmail_validToken_setsEmailVerifiedAndUsedAt() {
        User user = savedUser();
        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken("valid-token");
        token.setExpiresAt(Instant.now().plusSeconds(3600));

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("LOCAL");
        account.setProviderUserId("alice@example.com");
        account.setEmailVerified(false);

        when(emailVerificationTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "alice@example.com"))
                .thenReturn(Optional.of(account));
        when(emailVerificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(authAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.verifyEmail("valid-token");

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(account.isEmailVerified()).isTrue();
    }

    // U-06 : verifyEmail — token inconnu → 400
    @Test
    void verifyEmail_unknownToken_throws400() {
        when(emailVerificationTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.verifyEmail("bad-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // U-07 : verifyEmail — token déjà utilisé → 400
    @Test
    void verifyEmail_alreadyUsed_throws400() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("used-token");
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        token.setUsedAt(Instant.now().minusSeconds(60));

        when(emailVerificationTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("used-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // U-08 : verifyEmail — token expiré → 400
    @Test
    void verifyEmail_expired_throws400() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setToken("expired-token");
        token.setExpiresAt(Instant.now().minusSeconds(3600));

        when(emailVerificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.verifyEmail("expired-token"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
