package fr.ailegalcase.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuthLocaleInfraIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthAccountRepository authAccountRepository;

    @Autowired
    private EmailVerificationTokenRepository emailVerificationTokenRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    private User savedUser(String email) {
        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        return userRepository.save(user);
    }

    // I-01 : auth_accounts — email_verified vaut true par défaut (compte OAuth simulé)
    @Test
    void authAccount_emailVerifiedDefaultsToTrue() {
        User user = savedUser("oauth@example.com");

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-sub-999");
        AuthAccount saved = authAccountRepository.save(account);

        assertThat(saved.isEmailVerified()).isTrue();
    }

    // I-02 : auth_accounts — password_hash nullable pour comptes OAuth
    @Test
    void authAccount_passwordHashIsNullableForOAuth() {
        User user = savedUser("oauth2@example.com");

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("MICROSOFT");
        account.setProviderUserId("ms-sub-123");
        AuthAccount saved = authAccountRepository.save(account);

        assertThat(saved.getPasswordHash()).isNull();
    }

    // I-03 : auth_accounts — password_hash stocké pour compte LOCAL
    @Test
    void authAccount_passwordHashPersisted_forLocalAccount() {
        User user = savedUser("local@example.com");

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("LOCAL");
        account.setProviderUserId("local@example.com");
        account.setPasswordHash("$2a$10$hashedpassword");
        account.setEmailVerified(false);
        AuthAccount saved = authAccountRepository.save(account);

        Optional<AuthAccount> found = authAccountRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getPasswordHash()).isEqualTo("$2a$10$hashedpassword");
        assertThat(found.get().isEmailVerified()).isFalse();
    }

    // I-04 : email_verification_tokens — création et récupération par token
    @Test
    void emailVerificationToken_saveAndFindByToken() {
        User user = savedUser("verif@example.com");

        EmailVerificationToken token = new EmailVerificationToken();
        token.setUser(user);
        token.setToken("unique-verif-token-123");
        token.setExpiresAt(Instant.now().plusSeconds(86400));
        emailVerificationTokenRepository.save(token);

        Optional<EmailVerificationToken> found = emailVerificationTokenRepository.findByToken("unique-verif-token-123");
        assertThat(found).isPresent();
        assertThat(found.get().getUsedAt()).isNull();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    // I-05 : email_verification_tokens — token inconnu retourne vide
    @Test
    void emailVerificationToken_unknownToken_returnsEmpty() {
        Optional<EmailVerificationToken> found = emailVerificationTokenRepository.findByToken("nonexistent-token");
        assertThat(found).isEmpty();
    }

    // I-06 : password_reset_tokens — création et récupération par token
    @Test
    void passwordResetToken_saveAndFindByToken() {
        User user = savedUser("reset@example.com");

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("unique-reset-token-456");
        token.setExpiresAt(Instant.now().plusSeconds(86400));
        passwordResetTokenRepository.save(token);

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("unique-reset-token-456");
        assertThat(found).isPresent();
        assertThat(found.get().getUsedAt()).isNull();
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    // I-07 : password_reset_tokens — used_at peut être mis à jour
    @Test
    void passwordResetToken_usedAtCanBeSet() {
        User user = savedUser("used@example.com");

        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken("used-reset-token-789");
        token.setExpiresAt(Instant.now().plusSeconds(86400));
        PasswordResetToken saved = passwordResetTokenRepository.save(token);

        Instant usedAt = Instant.now();
        saved.setUsedAt(usedAt);
        passwordResetTokenRepository.save(saved);

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getUsedAt()).isNotNull();
    }
}
