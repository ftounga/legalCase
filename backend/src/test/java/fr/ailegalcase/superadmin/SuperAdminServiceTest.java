package fr.ailegalcase.superadmin;

import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuperAdminServiceTest {

    @Mock AuthAccountRepository authAccountRepository;
    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock UsageEventRepository usageEventRepository;
    @InjectMocks SuperAdminService service;

    // U-01 : listAllWorkspaces avec super-admin → retourne la liste de tous les workspaces
    @Test
    void listAllWorkspaces_asSuperAdmin_returnsAllWorkspaces() {
        User user = buildUser(true);
        AuthAccount account = buildAccount(user, "google-sa-sub");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sa-sub"))
                .thenReturn(Optional.of(account));

        Workspace ws = buildWorkspace("Cabinet Alpha");
        when(workspaceRepository.findAll()).thenReturn(List.of(ws));
        when(subscriptionRepository.findByWorkspaceId(ws.getId())).thenReturn(Optional.empty());
        when(workspaceMemberRepository.findByWorkspace_Id(ws.getId())).thenReturn(
                List.of(new WorkspaceMember(), new WorkspaceMember())
        );

        List<SuperAdminWorkspaceResponse> result = service.listAllWorkspaces(buildOidcUser("google-sa-sub"), "GOOGLE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Cabinet Alpha");
        assertThat(result.get(0).memberCount()).isEqualTo(2);
        assertThat(result.get(0).expiresAt()).isNull();
    }

    // U-03 : getUsageByWorkspace super-admin → agrégation correcte par workspace
    @Test
    void getUsageByWorkspace_asSuperAdmin_returnsAggregatedUsage() {
        User user = buildUser(true);
        AuthAccount account = buildAccount(user, "google-sa-usage-sub");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sa-usage-sub"))
                .thenReturn(Optional.of(account));

        UUID wsId = UUID.randomUUID();
        Workspace ws = buildWorkspace("Cabinet Usage");
        ws.setId(wsId);
        when(workspaceRepository.findAll()).thenReturn(List.of(ws));

        Object[] row = new Object[]{wsId.toString(), 5000L, 2000L, new BigDecimal("0.042000")};
        when(usageEventRepository.aggregateByWorkspaceId()).thenReturn(List.<Object[]>of(row));

        List<SuperAdminUsageResponse> result = service.getUsageByWorkspace(
                buildOidcUser("google-sa-usage-sub"), "GOOGLE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).workspaceName()).isEqualTo("Cabinet Usage");
        assertThat(result.get(0).totalTokensInput()).isEqualTo(5000L);
        assertThat(result.get(0).totalTokensOutput()).isEqualTo(2000L);
        assertThat(result.get(0).totalCost()).isEqualByComparingTo(new BigDecimal("0.042000"));
    }

    // U-04 : getUsageByWorkspace workspace sans usage → totalCost = 0
    @Test
    void getUsageByWorkspace_workspaceWithNoUsage_returnsZeros() {
        User user = buildUser(true);
        AuthAccount account = buildAccount(user, "google-sa-empty-sub");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sa-empty-sub"))
                .thenReturn(Optional.of(account));

        Workspace ws = buildWorkspace("Cabinet Vide");
        when(workspaceRepository.findAll()).thenReturn(List.of(ws));
        when(usageEventRepository.aggregateByWorkspaceId()).thenReturn(List.<Object[]>of());

        List<SuperAdminUsageResponse> result = service.getUsageByWorkspace(
                buildOidcUser("google-sa-empty-sub"), "GOOGLE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).totalTokensInput()).isZero();
        assertThat(result.get(0).totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // U-05 : deleteWorkspace — workspace inexistant → 404
    @Test
    void deleteWorkspace_unknownWorkspace_throws404() {
        User user = buildUser(true);
        AuthAccount account = buildAccount(user, "google-sa-del-sub");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-sa-del-sub"))
                .thenReturn(Optional.of(account));

        UUID unknownId = UUID.randomUUID();
        when(workspaceRepository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteWorkspace(buildOidcUser("google-sa-del-sub"), "GOOGLE", unknownId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Workspace not found");
    }

    // U-02 : listAllWorkspaces sans super-admin → 403
    @Test
    void listAllWorkspaces_withoutSuperAdmin_throws403() {
        User user = buildUser(false);
        AuthAccount account = buildAccount(user, "google-regular-sub");
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "google-regular-sub"))
                .thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.listAllWorkspaces(buildOidcUser("google-regular-sub"), "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Super-admin access required");
    }

    private User buildUser(boolean superAdmin) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setStatus("ACTIVE");
        user.setSuperAdmin(superAdmin);
        return user;
    }

    private AuthAccount buildAccount(User user, String sub) {
        AuthAccount account = new AuthAccount();
        account.setUser(user);
        account.setProvider("GOOGLE");
        account.setProviderUserId(sub);
        return account;
    }

    private Workspace buildWorkspace(String name) {
        Workspace ws = new Workspace();
        ws.setId(UUID.randomUUID());
        ws.setName(name);
        ws.setSlug("cabinet-alpha");
        ws.setPlanCode("STARTER");
        ws.setStatus("ACTIVE");
        return ws;
    }

    private OidcUser buildOidcUser(String sub) {
        Map<String, Object> claims = Map.of(
                "sub", sub, "email", "user@test.com", "iss", "https://accounts.google.com"
        );
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(3600), claims);
        return new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
    }
}
