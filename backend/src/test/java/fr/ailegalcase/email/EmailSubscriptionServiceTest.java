package fr.ailegalcase.email;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-248 SF-248-01 : tests unitaires du service de désabonnement des emails
 * non-transactionnels.
 */
@ExtendWith(MockitoExtension.class)
class EmailSubscriptionServiceTest {

    @Mock private UserRepository userRepository;

    private EmailSubscriptionService service;
    private User user;
    private UUID token;

    @BeforeEach
    void setUp() {
        service = new EmailSubscriptionService(userRepository);
        token = UUID.randomUUID();
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("alice@example.com");
        user.setStatus("ACTIVE");
        user.setMarketingUnsubscribeToken(token);
    }

    // UT-01 : unsubscribe avec un token valide → optedOut=true
    @Test
    void unsubscribe_validToken_setsOptedOutTrue() {
        when(userRepository.findByMarketingUnsubscribeToken(token)).thenReturn(Optional.of(user));

        boolean optedOut = service.unsubscribe(token.toString());

        assertThat(optedOut).isTrue();
        assertThat(user.isMarketingEmailsOptedOut()).isTrue();
        verify(userRepository).save(user);
    }

    // UT-02 : resubscribe → optedOut=false
    @Test
    void resubscribe_setsOptedOutFalse() {
        user.setMarketingEmailsOptedOut(true);
        when(userRepository.findByMarketingUnsubscribeToken(token)).thenReturn(Optional.of(user));

        boolean optedOut = service.resubscribe(token.toString());

        assertThat(optedOut).isFalse();
        assertThat(user.isMarketingEmailsOptedOut()).isFalse();
        verify(userRepository).save(user);
    }

    // UT-03a : token inconnu → 404
    @Test
    void unsubscribe_unknownToken_throws404() {
        when(userRepository.findByMarketingUnsubscribeToken(token)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.unsubscribe(token.toString()))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // UT-03b : token absent → 400
    @Test
    void unsubscribe_blankToken_throws400() {
        assertThatThrownBy(() -> service.unsubscribe("  "))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // UT-03c : token null → 400
    @Test
    void unsubscribe_nullToken_throws400() {
        assertThatThrownBy(() -> service.unsubscribe(null))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // UT-03d : token mal formé (pas un UUID) → 400
    @Test
    void unsubscribe_malformedToken_throws400() {
        assertThatThrownBy(() -> service.unsubscribe("pas-un-uuid"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // UT-04 : unsubscribe idempotent — 2 appels → toujours optedOut=true
    @Test
    void unsubscribe_calledTwice_isIdempotent() {
        when(userRepository.findByMarketingUnsubscribeToken(token)).thenReturn(Optional.of(user));

        assertThat(service.unsubscribe(token.toString())).isTrue();
        assertThat(service.unsubscribe(token.toString())).isTrue();
        assertThat(user.isMarketingEmailsOptedOut()).isTrue();
        // second appel : déjà désinscrit → pas de save inutile
        verify(userRepository).save(user);
    }

    // UT-05 : subscription-status renvoie l'état courant
    @Test
    void subscriptionStatus_returnsCurrentState() {
        when(userRepository.findByMarketingUnsubscribeToken(token)).thenReturn(Optional.of(user));

        assertThat(service.subscriptionStatus(token.toString())).isFalse();

        user.setMarketingEmailsOptedOut(true);
        assertThat(service.subscriptionStatus(token.toString())).isTrue();
    }

    // UT-06 : resubscribe sur un utilisateur déjà inscrit → idempotent, pas de save
    @Test
    void resubscribe_alreadySubscribed_isIdempotentNoSave() {
        lenient().when(userRepository.findByMarketingUnsubscribeToken(token)).thenReturn(Optional.of(user));

        assertThat(service.resubscribe(token.toString())).isFalse();
        verify(userRepository, never()).save(user);
    }
}
