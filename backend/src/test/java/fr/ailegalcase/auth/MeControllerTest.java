package fr.ailegalcase.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeControllerTest {

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private OidcUser oidcUser;

    @Mock
    private OAuth2AuthenticationToken authToken;

    private MeController controller;

    @BeforeEach
    void setUp() {
        controller = new MeController(authAccountRepository);
    }

    // U-01 : utilisateur authentifié → retourne MeResponse avec les bons champs
    @Test
    void me_authenticatedUser_returnsMeResponse() {
        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setEmail("john@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setStatus("ACTIVE");


        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId("google-sub-123");

        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(account));

        // Set id via reflection since @PrePersist won't run in unit test
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, userId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        MeResponse response = controller.me(oidcUser, authToken);

        assertThat(response.email()).isEqualTo("john@example.com");
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.provider()).isEqualTo("GOOGLE");
        assertThat(response.id()).isEqualTo(userId);
    }

    // U-02 : AuthAccount introuvable → exception
    @Test
    void me_accountNotFound_throwsException() {
        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(authToken.getAuthorizedClientRegistrationId()).thenReturn("google");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.me(oidcUser, authToken))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("Auth account not found");
    }
}
