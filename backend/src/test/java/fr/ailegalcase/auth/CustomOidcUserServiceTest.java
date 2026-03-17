package fr.ailegalcase.auth;

import fr.ailegalcase.workspace.WorkspaceService;
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

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthAccountRepository authAccountRepository;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private OidcUser oidcUser;

    private CustomOidcUserService service;

    @BeforeEach
    void setUp() {
        service = new CustomOidcUserService(userRepository, authAccountRepository, workspaceService);
    }

    // U-01 : premier login — claims complets → user + auth_account créés
    @Test
    void findOrCreateUser_firstLogin_createsUserAndAuthAccount() {
        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(oidcUser.getEmail()).thenReturn("john@example.com");
        when(oidcUser.getGivenName()).thenReturn("John");
        when(oidcUser.getFamilyName()).thenReturn("Doe");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.empty());
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

    // U-02 : login existant — aucun doublon créé
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

    // U-05 : deux providers, même email → deux users distincts créés
    @Test
    void findOrCreateUser_twoProviders_sameEmail_createsTwoUsers() {
        when(oidcUser.getEmail()).thenReturn("john@example.com");
        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(authAccountRepository.findByProviderAndProviderUserId(eq("GOOGLE"), eq("google-sub-123")))
                .thenReturn(Optional.empty());

        OidcUser msUser = mock(OidcUser.class);
        when(msUser.getSubject()).thenReturn("ms-sub-456");
        when(msUser.getEmail()).thenReturn("john@example.com");
        when(authAccountRepository.findByProviderAndProviderUserId(eq("MICROSOFT"), eq("ms-sub-456")))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authAccountRepository.save(any(AuthAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        service.findOrCreateUser(oidcUser, "GOOGLE", Set.of("openid"));
        service.findOrCreateUser(msUser, "MICROSOFT", Set.of("openid"));

        verify(userRepository, times(2)).save(any(User.class));
    }
}
