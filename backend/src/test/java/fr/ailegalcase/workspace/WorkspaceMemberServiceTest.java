package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceMemberServiceTest {

    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private AuthAccountRepository authAccountRepository;

    private WorkspaceMemberService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceMemberService(workspaceMemberRepository, authAccountRepository);
    }

    // U-01 : listMembers retourne les membres du workspace courant
    @Test
    void listMembers_returnsMembers() {
        User user = buildUser("u1@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember member = buildMember(workspace, user, "OWNER");

        AuthAccount account = new AuthAccount();
        account.setUser(user);
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub1")).thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
        when(workspaceMemberRepository.findByWorkspace_Id(workspace.getId())).thenReturn(List.of(member));

        List<WorkspaceMemberResponse> result = service.listMembers(buildOidcUser("sub1", "u1@example.com"), "GOOGLE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).memberRole()).isEqualTo("OWNER");
        assertThat(result.get(0).email()).isEqualTo("u1@example.com");
    }

    // U-02 : removeMember — OWNER révoque un autre membre → succès
    @Test
    void removeMember_ownerRemovesOtherMember_succeeds() {
        User owner = buildUser("owner@example.com");
        User target = buildUser("target@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");
        WorkspaceMember targetMember = buildMember(workspace, target, "LAWYER");

        AuthAccount account = new AuthAccount();
        account.setUser(owner);
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-owner")).thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), target.getId())).thenReturn(Optional.of(targetMember));

        service.removeMember(target.getId(), buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE");

        verify(workspaceMemberRepository).delete(targetMember);
    }

    // U-03 : removeMember — OWNER tente de se révoquer lui-même → 403
    @Test
    void removeMember_ownerRevokesThemself_throws403() {
        User owner = buildUser("owner@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember ownerMember = buildMember(workspace, owner, "OWNER");

        AuthAccount account = new AuthAccount();
        account.setUser(owner);
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-owner")).thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(owner)).thenReturn(Optional.of(ownerMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), owner.getId())).thenReturn(Optional.of(ownerMember));

        assertThatThrownBy(() -> service.removeMember(owner.getId(), buildOidcUser("sub-owner", "owner@example.com"), "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-04 : removeMember — ADMIN tente de révoquer → 403
    @Test
    void removeMember_adminRole_throws403() {
        User admin = buildUser("admin@example.com");
        User target = buildUser("target@example.com");
        Workspace workspace = buildWorkspace();
        WorkspaceMember adminMember = buildMember(workspace, admin, "ADMIN");

        AuthAccount account = new AuthAccount();
        account.setUser(admin);
        when(authAccountRepository.findByProviderAndProviderUserId("GOOGLE", "sub-admin")).thenReturn(Optional.of(account));
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(admin)).thenReturn(Optional.of(adminMember));
        when(workspaceMemberRepository.findByWorkspace_IdAndUser_Id(workspace.getId(), admin.getId())).thenReturn(Optional.of(adminMember));

        assertThatThrownBy(() -> service.removeMember(target.getId(), buildOidcUser("sub-admin", "admin@example.com"), "GOOGLE"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // Helpers

    private User buildUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setFirstName("First");
        u.setLastName("Last");
        u.setStatus("ACTIVE");
        return u;
    }

    private Workspace buildWorkspace() {
        Workspace w = new Workspace();
        w.setId(UUID.randomUUID());
        w.setName("Test workspace");
        w.setSlug("test-slug");
        w.setPlanCode("STARTER");
        w.setStatus("ACTIVE");
        return w;
    }

    private WorkspaceMember buildMember(Workspace workspace, User user, String role) {
        WorkspaceMember m = new WorkspaceMember();
        m.setWorkspace(workspace);
        m.setUser(user);
        m.setMemberRole(role);
        m.setPrimary(true);
        return m;
    }

    private OidcUser buildOidcUser(String sub, String email) {
        Map<String, Object> claims = Map.of("sub", sub, "email", email,
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        return new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
    }
}
