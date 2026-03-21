package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceGetCurrentTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private fr.ailegalcase.billing.SubscriptionRepository subscriptionRepository;
    @Mock private fr.ailegalcase.billing.StripeCustomerService stripeCustomerService;
    @Mock private OidcUser oidcUser;

    private WorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceService(workspaceRepository, workspaceMemberRepository,
                authAccountRepository, subscriptionRepository, stripeCustomerService);
    }

    // U-01 : utilisateur avec workspace → retourne WorkspaceResponse
    @Test
    void getCurrentWorkspace_withWorkspace_returnsResponse() {
        User user = new User();
        user.setEmail("john@example.com");

        Workspace workspace = new Workspace();
        workspace.setName("john@example.com");
        workspace.setSlug("some-slug");
        workspace.setPlanCode("STARTER");
        workspace.setStatus("ACTIVE");

        try {
            var idField = Workspace.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(workspace, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        WorkspaceMember member = new WorkspaceMember();
        member.setUser(user);
        member.setWorkspace(workspace);

        AuthAccount account = new AuthAccount();
        account.setUser(user);

        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        WorkspaceResponse response = service.getCurrentWorkspace(oidcUser, "GOOGLE", null);

        assertThat(response.name()).isEqualTo("john@example.com");
        assertThat(response.planCode()).isEqualTo("STARTER");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    // U-02 : utilisateur sans workspace → 404
    @Test
    void getCurrentWorkspace_noWorkspace_throws404() {
        User user = new User();
        AuthAccount account = new AuthAccount();
        account.setUser(user);

        when(oidcUser.getSubject()).thenReturn("google-sub-123");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sub-123"))
                .thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCurrentWorkspace(oidcUser, "GOOGLE", null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(NOT_FOUND));
    }
}
