package fr.ailegalcase.audit;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class AuditLogAdminServiceTest {

    private final AuditLogRepository auditLogRepo = mock(AuditLogRepository.class);
    private final WorkspaceMemberRepository memberRepo = mock(WorkspaceMemberRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

    private final AuditLogAdminService service = new AuditLogAdminService(
            auditLogRepo, memberRepo, userRepo, currentUserResolver);

    // U-01 : OWNER → retourne les logs de son workspace
    @Test
    void getAuditLogs_owner_returnsLogs() {
        var ctx = buildContext("OWNER");
        UUID logId = UUID.randomUUID();
        AuditLog log = auditLog(logId, ctx.workspace.getId(), ctx.user.getId(),
                "{\"caseFileTitle\":\"Licenciement Dupont\"}");
        when(auditLogRepo.findTop50ByWorkspaceIdOrderByCreatedAtDesc(ctx.workspace.getId()))
                .thenReturn(List.of(log));
        when(userRepo.findAllById(anyCollection())).thenReturn(List.of(ctx.user));

        List<AuditLogResponse> result = service.getAuditLogs(ctx.oidcUser, ctx.auth);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(logId);
        assertThat(result.get(0).userEmail()).isEqualTo(ctx.user.getEmail());
        assertThat(result.get(0).caseFileTitle()).isEqualTo("Licenciement Dupont");
        assertThat(result.get(0).action()).isEqualTo("DOCUMENT_DELETED");
    }

    // U-02 : MEMBER → 403
    @Test
    void getAuditLogs_member_throws403() {
        var ctx = buildContext("MEMBER");
        assertThatThrownBy(() -> service.getAuditLogs(ctx.oidcUser, ctx.auth))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-03 : isolation workspace — logs d'un autre workspace non retournés
    @Test
    void getAuditLogs_isolationWorkspace_onlyReturnsOwnLogs() {
        var ctx = buildContext("OWNER");
        UUID otherWorkspaceId = UUID.randomUUID();
        AuditLog otherLog = auditLog(UUID.randomUUID(), otherWorkspaceId, UUID.randomUUID(), null);

        // repo filtre par workspace → retourne liste vide pour ce workspace
        when(auditLogRepo.findTop50ByWorkspaceIdOrderByCreatedAtDesc(ctx.workspace.getId()))
                .thenReturn(List.of());

        List<AuditLogResponse> result = service.getAuditLogs(ctx.oidcUser, ctx.auth);

        assertThat(result).isEmpty();
        verify(auditLogRepo).findTop50ByWorkspaceIdOrderByCreatedAtDesc(ctx.workspace.getId());
        verify(auditLogRepo, never()).findTop50ByWorkspaceIdOrderByCreatedAtDesc(otherWorkspaceId);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private record TestContext(User user, Workspace workspace, DefaultOidcUser oidcUser,
                               OAuth2AuthenticationToken auth) {}

    private TestContext buildContext(String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail("owner@example.com");
        u.setStatus("ACTIVE");

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(u);

        Workspace ws = new Workspace();
        ws.setId(UUID.randomUUID());

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(u);
        member.setMemberRole(role);
        when(memberRepo.findByUserAndPrimaryTrue(u)).thenReturn(Optional.of(member));

        Map<String, Object> claims = Map.of("sub", "google-sub", "email", "owner@example.com",
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");

        return new TestContext(u, ws, oidcUser, auth);
    }

    private AuditLog auditLog(UUID id, UUID workspaceId, UUID userId, String metadata) {
        AuditLog log = new AuditLog();
        log.setId(id);
        log.setWorkspaceId(workspaceId);
        log.setUserId(userId);
        log.setCaseFileId(UUID.randomUUID());
        log.setAction("DOCUMENT_DELETED");
        log.setMetadata(metadata);
        return log;
    }
}
