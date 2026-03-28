package fr.ailegalcase.audit;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    private static final PageRequest DEFAULT_PAGE = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

    // U-01 : OWNER → retourne les logs de son workspace
    @Test
    void getAuditLogs_owner_returnsLogs() {
        var ctx = buildContext("OWNER");
        UUID logId = UUID.randomUUID();
        AuditLog log = auditLog(logId, ctx.workspace.getId(), ctx.user.getId(),
                "{\"caseFileTitle\":\"Licenciement Dupont\"}");
        when(auditLogRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(log)));
        when(userRepo.findAllById(anyCollection())).thenReturn(List.of(ctx.user));

        Page<AuditLogResponse> result = service.getAuditLogs(ctx.oidcUser, ctx.auth, null, null, null, DEFAULT_PAGE);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).id()).isEqualTo(logId);
        assertThat(result.getContent().get(0).userEmail()).isEqualTo(ctx.user.getEmail());
        assertThat(result.getContent().get(0).caseFileTitle()).isEqualTo("Licenciement Dupont");
        assertThat(result.getContent().get(0).action()).isEqualTo("DOCUMENT_DELETED");
    }

    // U-02 : MEMBER → 403
    @Test
    void getAuditLogs_member_throws403() {
        var ctx = buildContext("MEMBER");
        assertThatThrownBy(() -> service.getAuditLogs(ctx.oidcUser, ctx.auth, null, null, null, DEFAULT_PAGE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-03 : isolation workspace — findAll appelé avec Specification (contient le workspaceId)
    @Test
    void getAuditLogs_isolationWorkspace_callsFindAllWithSpec() {
        var ctx = buildContext("OWNER");
        when(auditLogRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(ctx.oidcUser, ctx.auth, null, null, null, DEFAULT_PAGE);

        verify(auditLogRepo).findAll(any(Specification.class), any(PageRequest.class));
        verify(auditLogRepo, never()).findTop50ByWorkspaceIdOrderByCreatedAtDesc(any());
    }

    // U-09 : from > to → 400 Bad Request
    @Test
    void getAuditLogs_fromAfterTo_throws400() {
        var ctx = buildContext("OWNER");
        Instant from = Instant.parse("2026-03-31T00:00:00Z");
        Instant to   = Instant.parse("2026-03-01T00:00:00Z");

        assertThatThrownBy(() -> service.getAuditLogs(ctx.oidcUser, ctx.auth, from, to, null, DEFAULT_PAGE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
    }

    // U-10 : sans params → findAll appelé avec Pageable par défaut
    @Test
    void getAuditLogs_noParams_callsFindAllWithPageable() {
        var ctx = buildContext("OWNER");
        when(auditLogRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(ctx.oidcUser, ctx.auth, null, null, null, DEFAULT_PAGE);

        verify(auditLogRepo).findAll(any(Specification.class), eq(DEFAULT_PAGE));
        verify(auditLogRepo, never()).findTop50ByWorkspaceIdOrderByCreatedAtDesc(any());
    }

    // U-11 : action filter → findAll appelé (Specification inclut le filtre action)
    @Test
    void getAuditLogs_withAction_callsFindAll() {
        var ctx = buildContext("OWNER");
        when(auditLogRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(ctx.oidcUser, ctx.auth, null, null, "DOCUMENT_DELETED", DEFAULT_PAGE);

        verify(auditLogRepo).findAll(any(Specification.class), eq(DEFAULT_PAGE));
    }

    // U-12 : from + to + action + pageable — combinaison complète → findAll appelé une fois
    @Test
    void getAuditLogs_allFilters_callsFindAllOnce() {
        var ctx = buildContext("OWNER");
        Instant from = Instant.parse("2026-03-01T00:00:00Z");
        Instant to   = Instant.parse("2026-03-31T23:59:59Z");
        when(auditLogRepo.findAll(any(Specification.class), any(PageRequest.class)))
                .thenReturn(Page.empty());

        service.getAuditLogs(ctx.oidcUser, ctx.auth, from, to, "DOCUMENT_DELETED", DEFAULT_PAGE);

        verify(auditLogRepo, times(1)).findAll(any(Specification.class), eq(DEFAULT_PAGE));
    }

    // U-04 : exportCsv — retourne CSV avec header + toutes les entrées
    @Test
    void exportCsv_returnsCsvWithAllEntries() {
        var ctx = buildContext("OWNER");
        Instant ts = Instant.parse("2026-03-28T10:00:00Z");
        AuditLog log1 = auditLogWithDate(UUID.randomUUID(), ctx.workspace.getId(), ctx.user.getId(),
                "{\"caseFileTitle\":\"Dossier A\"}", ts);
        AuditLog log2 = auditLogWithDate(UUID.randomUUID(), ctx.workspace.getId(), ctx.user.getId(),
                "{\"documentName\":\"contrat.pdf\"}", ts);
        when(auditLogRepo.findAllByWorkspaceIdOrderByCreatedAtDesc(ctx.workspace.getId()))
                .thenReturn(List.of(log1, log2));
        when(userRepo.findAllById(anyCollection())).thenReturn(List.of(ctx.user));

        byte[] result = service.exportCsv(ctx.oidcUser, ctx.auth);
        String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("Date,Action,Utilisateur,Dossier,Document");
        assertThat(csv).contains("owner@example.com");
        assertThat(csv).contains("Dossier A");
        assertThat(csv).contains("contrat.pdf");
        long lines = csv.lines().filter(l -> !l.isBlank()).count();
        assertThat(lines).isEqualTo(3);
    }

    // U-05 : exportCsv — champ avec virgule → entouré de guillemets (RFC 4180)
    @Test
    void exportCsv_escapesFieldWithComma() {
        var ctx = buildContext("OWNER");
        Instant ts = Instant.parse("2026-03-28T10:00:00Z");
        AuditLog log = auditLogWithDate(UUID.randomUUID(), ctx.workspace.getId(), ctx.user.getId(),
                "{\"caseFileTitle\":\"Dupont, Marie\"}", ts);
        when(auditLogRepo.findAllByWorkspaceIdOrderByCreatedAtDesc(ctx.workspace.getId()))
                .thenReturn(List.of(log));
        when(userRepo.findAllById(anyCollection())).thenReturn(List.of(ctx.user));

        byte[] result = service.exportCsv(ctx.oidcUser, ctx.auth);
        String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("\"Dupont, Marie\"");
    }

    // U-06 : exportCsv — journal vide → seulement la ligne d'en-tête
    @Test
    void exportCsv_emptyJournal_returnsHeaderOnly() {
        var ctx = buildContext("OWNER");
        when(auditLogRepo.findAllByWorkspaceIdOrderByCreatedAtDesc(ctx.workspace.getId()))
                .thenReturn(List.of());

        byte[] result = service.exportCsv(ctx.oidcUser, ctx.auth);
        String csv = new String(result, java.nio.charset.StandardCharsets.UTF_8);

        assertThat(csv).contains("Date,Action,Utilisateur,Dossier,Document");
        long lines = csv.lines().filter(l -> !l.isBlank()).count();
        assertThat(lines).isEqualTo(1);
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
        log.setCreatedAt(Instant.now());
        return log;
    }

    private AuditLog auditLogWithDate(UUID id, UUID workspaceId, UUID userId, String metadata, Instant createdAt) {
        AuditLog log = auditLog(id, workspaceId, userId, metadata);
        log.setCreatedAt(createdAt);
        return log;
    }
}
