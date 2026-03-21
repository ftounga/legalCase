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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ForgotResetPasswordServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    private LocalAuthService service;

    @BeforeEach
    void setUp() {
        service = new LocalAuthService(userRepository, authAccountRepository,
                emailVerificationTokenRepository, passwordResetTokenRepository,
                passwordEncoder, emailService);
    }

    private AuthAccount localAccount(String email) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");


        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("LOCAL");
        account.setProviderUserId(email);
        account.setEmailVerified(true);
        return account;
    }

    private PasswordResetToken validToken(String email, String tokenValue) {
        User user = new User();
        user.setEmail(email);

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(tokenValue);
        token.setExpiresAt(Instant.now().plusSeconds(3600));
        return token;
    }

    // U-01 : forgotPassword — compte LOCAL existant → token créé, email envoyé
    @Test
    void forgotPassword_localAccountExists_createsTokenAndSendsEmail() {
        AuthAccount account = localAccount("alice@example.com");
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "alice@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.forgotPassword(new ForgotPasswordRequest("alice@example.com"));

        ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getToken()).isNotBlank();
        assertThat(captor.getValue().getExpiresAt()).isAfter(Instant.now());

        verify(emailService).sendPasswordReset(eq("alice@example.com"), anyString());
    }

    // U-02 : forgotPassword — email inconnu → 200 silencieux, aucun token créé
    @Test
    void forgotPassword_unknownEmail_silentNoOp() {
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatNoException().isThrownBy(
                () -> service.forgotPassword(new ForgotPasswordRequest("unknown@example.com")));

        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordReset(anyString(), anyString());
    }

    // U-03 : forgotPassword — fail-open email → réussit même si envoi échoue
    @Test
    void forgotPassword_emailSendFails_succeedsAnyway() {
        AuthAccount account = localAccount("alice@example.com");
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "alice@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new MailSendException("SMTP error")).when(emailService).sendPasswordReset(anyString(), anyString());

        assertThatNoException().isThrownBy(
                () -> service.forgotPassword(new ForgotPasswordRequest("alice@example.com")));

        verify(passwordResetTokenRepository).save(any());
    }

    // U-04 : resetPassword — token valide → passwordHash mis à jour, usedAt renseigné
    @Test
    void resetPassword_validToken_updatesPasswordHash() {
        PasswordResetToken token = validToken("alice@example.com", "valid-token");
        AuthAccount account = localAccount("alice@example.com");

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(token));
        when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "alice@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.encode("newpassword")).thenReturn("$2a$10$newhash");
        when(passwordResetTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(authAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resetPassword(new ResetPasswordRequest("valid-token", "newpassword"));

        assertThat(token.getUsedAt()).isNotNull();
        assertThat(account.getPasswordHash()).isEqualTo("$2a$10$newhash");
    }

    // U-05 : resetPassword — token inconnu → 400
    @Test
    void resetPassword_unknownToken_throws400() {
        when(passwordResetTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("bad-token", "newpassword")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // U-06 : resetPassword — token déjà utilisé → 400
    @Test
    void resetPassword_alreadyUsed_throws400() {
        PasswordResetToken token = validToken("alice@example.com", "used-token");
        token.setUsedAt(Instant.now().minusSeconds(60));

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("used-token", "newpassword")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // U-07 : resetPassword — token expiré → 400
    @Test
    void resetPassword_expired_throws400() {
        PasswordResetToken token = validToken("alice@example.com", "expired-token");
        token.setExpiresAt(Instant.now().minusSeconds(3600));

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> service.resetPassword(new ResetPasswordRequest("expired-token", "newpassword")))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
