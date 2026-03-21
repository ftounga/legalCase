package fr.ailegalcase.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomOidcUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private OidcUser oidcUser;

    private CustomOidcUserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOidcUserService(userRepository, authAccountRepository);
    }

    // U-01 : premier login OAuth, email inconnu → User + AuthAccount créés
    @Test
    void findOrCreateUser_firstLogin_createsUserAndAuthAccount() {
        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(oidcUser.getEmail()).thenReturn("john@example.com");
        when(oidcUser.getGivenName()).thenReturn("John");
        when(oidcUser.getFamilyName()).thenReturn("Doe");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authAccountRepository.save(any(AuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.findOrCreateUser(oidcUser, "GOOGLE", Set.of("openid", "email", "profile"));

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("john@example.com");
        assertThat(userCaptor.getValue().getFirstName()).isEqualTo("John");
        assertThat(userCaptor.getValue().getLastName()).isEqualTo("Doe");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo("ACTIVE");

        ArgumentCaptor<AuthAccount> accountCaptor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getProvider()).isEqualTo("GOOGLE");
        assertThat(accountCaptor.getValue().getProviderUserId()).isEqualTo("google-sub-123");
        assertThat(accountCaptor.getValue().getProviderEmail()).isEqualTo("john@example.com");
    }

    // U-02 : login OAuth existant (même sub) → aucun doublon créé
    @Test
    void findOrCreateUser_existingLogin_doesNotCreateDuplicate() {
        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(oidcUser.getEmail()).thenReturn("john@example.com");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(new AuthAccount()));

        service.findOrCreateUser(oidcUser, "GOOGLE", Set.of("openid", "email"));

        verify(userRepository, never()).save(any());
        verify(authAccountRepository, never()).save(any());
    }

    // U-03 : claim sub absent → exception
    @Test
    void findOrCreateUser_missingSub_throwsException() {
        when(oidcUser.getSubject()).thenReturn(null);

        assertThatThrownBy(() -> service.findOrCreateUser(oidcUser, "GOOGLE", Set.of()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("sub");
    }

    // U-04 : claim email absent → exception
    @Test
    void findOrCreateUser_missingEmail_throwsException() {
        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(oidcUser.getEmail()).thenReturn(null);

        assertThatThrownBy(() -> service.findOrCreateUser(oidcUser, "GOOGLE", Set.of()))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("email");
    }

    // U-05 : fusion OAuth→LOCAL — email LOCAL existant → User réutilisé, seul AuthAccount OAuth créé
    @Test
    void findOrCreateUser_localUserExists_fusesAccount() {
        User existingUser = new User();
        existingUser.setEmail("local@example.com");
        existingUser.setStatus("ACTIVE");


        when(oidcUser.getSubject()).thenReturn("google-sub-fusion");
        when(oidcUser.getEmail()).thenReturn("local@example.com");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-fusion"))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("local@example.com")).thenReturn(Optional.of(existingUser));
        when(authAccountRepository.save(any(AuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.findOrCreateUser(oidcUser, "GOOGLE", Set.of("openid"));

        // Pas de création de User (déjà existant)
        verify(userRepository, never()).save(any());

        // AuthAccount OAuth créé et rattaché au User existant
        ArgumentCaptor<AuthAccount> accountCaptor = ArgumentCaptor.forClass(AuthAccount.class);
        verify(authAccountRepository).save(accountCaptor.capture());
        assertThat(accountCaptor.getValue().getProvider()).isEqualTo("GOOGLE");
        assertThat(accountCaptor.getValue().getUser()).isSameAs(existingUser);
    }

    // U-06 : deux providers OAuth, même email inconnu → 1 User créé au 1er login, fusion au 2nd
    @Test
    void findOrCreateUser_twoOAuthProvidersSameEmail_sharesSameUser() {
        when(oidcUser.getEmail()).thenReturn("john@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(oidcUser.getGivenName()).thenReturn("John");
        when(oidcUser.getFamilyName()).thenReturn("Doe");
        when(authAccountRepository.findByProviderAndProviderUserId(eq("GOOGLE"), eq("google-sub-123")))
                .thenReturn(Optional.empty());
        // Premier login : email inconnu → crée User
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

        User createdUser = new User();
        createdUser.setEmail("john@example.com");
        when(userRepository.save(any(User.class))).thenReturn(createdUser);
        when(authAccountRepository.save(any(AuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.findOrCreateUser(oidcUser, "GOOGLE", Set.of("openid"));

        // Deuxième login MS avec même email → fusion : User existant trouvé
        OidcUser msUser = mock(OidcUser.class);
        when(msUser.getSubject()).thenReturn("ms-sub-456");
        when(msUser.getEmail()).thenReturn("john@example.com");
        when(authAccountRepository.findByProviderAndProviderUserId(eq("MICROSOFT"), eq("ms-sub-456")))
                .thenReturn(Optional.empty());
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(createdUser));

        service.findOrCreateUser(msUser, "MICROSOFT", Set.of("openid"));

        // User créé une seule fois (premier login) ; fusion au second
        verify(userRepository, times(1)).save(any(User.class));
        verify(authAccountRepository, times(2)).save(any(AuthAccount.class));
    }
}
