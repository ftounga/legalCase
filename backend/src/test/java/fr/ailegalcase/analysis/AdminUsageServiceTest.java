package fr.ailegalcase.analysis;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminUsageServiceTest {

    private final AuthAccountRepository authAccountRepo = mock(AuthAccountRepository.class);
    private final WorkspaceMemberRepository memberRepo = mock(WorkspaceMemberRepository.class);
    private final CaseFileRepository caseFileRepo = mock(CaseFileRepository.class);
    private final UsageEventRepository usageEventRepo = mock(UsageEventRepository.class);
    private final UserRepository userRepo = mock(UserRepository.class);
    private final PlanLimitService planLimitService = mock(PlanLimitService.class);
    private final CurrentUserResolver currentUserResolver = mock(CurrentUserResolver.class);

    private final AdminUsageService service = new AdminUsageService(
            authAccountRepo, memberRepo, caseFileRepo, usageEventRepo, userRepo, planLimitService, currentUserResolver);

    // U-01 : OWNER → retourne summary avec totaux corrects
    @Test
    void getWorkspaceSummary_owner_returnsSummary() {
        var ctx = buildContext("OWNER");
        UUID cfId = UUID.randomUUID();
        CaseFile cf = caseFile(cfId, ctx.workspace, "Dossier 1");
        when(caseFileRepo.findByWorkspace_Id(ctx.workspace.getId())).thenReturn(List.of(cf));

        UsageEvent event = event(cfId, ctx.user.getId(), 100, 50, new BigDecimal("0.001000"));
        when(usageEventRepo.findByCaseFileIdIn(anyCollection())).thenReturn(List.of(event));
        when(userRepo.findAllById(anyCollection())).thenReturn(List.of(ctx.user));

        WorkspaceUsageSummaryResponse result = service.getWorkspaceSummary(ctx.oidcUser, ctx.auth);

        assertThat(result.totalTokensInput()).isEqualTo(100);
        assertThat(result.totalTokensOutput()).isEqualTo(50);
        assertThat(result.totalCost()).isEqualByComparingTo(new BigDecimal("0.001000"));
        assertThat(result.byUser()).hasSize(1);
        assertThat(result.byCaseFile()).hasSize(1);
        assertThat(result.byCaseFile().get(0).caseFileTitle()).isEqualTo("Dossier 1");
        assertThat(result.byUser().get(0).userEmail()).isEqualTo(ctx.user.getEmail());
    }

    // U-02 : ADMIN → même comportement qu'OWNER
    @Test
    void getWorkspaceSummary_admin_returnsSummary() {
        var ctx = buildContext("ADMIN");
        when(caseFileRepo.findByWorkspace_Id(ctx.workspace.getId())).thenReturn(List.of());

        WorkspaceUsageSummaryResponse result = service.getWorkspaceSummary(ctx.oidcUser, ctx.auth);

        assertThat(result.totalTokensInput()).isZero();
    }

    // U-03 : LAWYER → 403
    @Test
    void getWorkspaceSummary_lawyer_throws403() {
        var ctx = buildContext("LAWYER");
        assertThatThrownBy(() -> service.getWorkspaceSummary(ctx.oidcUser, ctx.auth))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-04 : MEMBER → 403
    @Test
    void getWorkspaceSummary_member_throws403() {
        var ctx = buildContext("MEMBER");
        assertThatThrownBy(() -> service.getWorkspaceSummary(ctx.oidcUser, ctx.auth))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    // U-05 : workspace sans events → totaux à 0, listes vides
    @Test
    void getWorkspaceSummary_noEvents_returnsZeros() {
        var ctx = buildContext("OWNER");
        UUID cfId = UUID.randomUUID();
        when(caseFileRepo.findByWorkspace_Id(ctx.workspace.getId()))
                .thenReturn(List.of(caseFile(cfId, ctx.workspace, "Vide")));
        when(usageEventRepo.findByCaseFileIdIn(anyCollection())).thenReturn(List.of());

        WorkspaceUsageSummaryResponse result = service.getWorkspaceSummary(ctx.oidcUser, ctx.auth);

        assertThat(result.totalTokensInput()).isZero();
        assertThat(result.totalCost()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.byUser()).isEmpty();
        assertThat(result.byCaseFile()).isEmpty();
    }

    // U-06 : agrégation byUser correcte avec 2 utilisateurs distincts
    @Test
    void getWorkspaceSummary_twoUsers_aggregatesCorrectly() {
        var ctx = buildContext("OWNER");
        UUID cfId = UUID.randomUUID();
        when(caseFileRepo.findByWorkspace_Id(ctx.workspace.getId()))
                .thenReturn(List.of(caseFile(cfId, ctx.workspace, "CF")));

        UUID userId2 = UUID.randomUUID();
        User user2 = user(userId2, "other@test.com");

        when(usageEventRepo.findByCaseFileIdIn(anyCollection())).thenReturn(List.of(
                event(cfId, ctx.user.getId(), 100, 50, new BigDecimal("0.001")),
                event(cfId, userId2, 200, 80, new BigDecimal("0.002"))
        ));
        when(userRepo.findAllById(anyCollection())).thenReturn(List.of(ctx.user, user2));

        WorkspaceUsageSummaryResponse result = service.getWorkspaceSummary(ctx.oidcUser, ctx.auth);

        assertThat(result.byUser()).hasSize(2);
        assertThat(result.totalTokensInput()).isEqualTo(300);
    }

    // U-07 : agrégation byCaseFile correcte avec 2 dossiers distincts
    @Test
    void getWorkspaceSummary_twoCaseFiles_aggregatesCorrectly() {
        var ctx = buildContext("OWNER");
        UUID cfId1 = UUID.randomUUID();
        UUID cfId2 = UUID.randomUUID();
        when(caseFileRepo.findByWorkspace_Id(ctx.workspace.getId())).thenReturn(List.of(
                caseFile(cfId1, ctx.workspace, "Dossier A"),
                caseFile(cfId2, ctx.workspace, "Dossier B")
        ));
        when(usageEventRepo.findByCaseFileIdIn(anyCollection())).thenReturn(List.of(
                event(cfId1, ctx.user.getId(), 100, 50, new BigDecimal("0.001")),
                event(cfId2, ctx.user.getId(), 300, 100, new BigDecimal("0.003"))
        ));
        when(userRepo.findAllById(anyCollection())).thenReturn(List.of(ctx.user));

        WorkspaceUsageSummaryResponse result = service.getWorkspaceSummary(ctx.oidcUser, ctx.auth);

        assertThat(result.byCaseFile()).hasSize(2);
        assertThat(result.totalTokensInput()).isEqualTo(400);
    }

    // U-08 (SF-34-02) : monthlyTokensUsed et monthlyTokensBudget présents dans la réponse
    @Test
    void getWorkspaceSummary_returnsMonthlyTokensAndBudget() {
        var ctx = buildContext("OWNER");
        when(caseFileRepo.findByWorkspace_Id(ctx.workspace.getId())).thenReturn(List.of());
        when(usageEventRepo.sumTokensByWorkspaceIdSince(eq(ctx.workspace.getId()), any(Instant.class)))
                .thenReturn(123_456L);
        when(planLimitService.getMonthlyTokenBudgetForWorkspace(ctx.workspace.getId()))
                .thenReturn(3_000_000L);

        WorkspaceUsageSummaryResponse result = service.getWorkspaceSummary(ctx.oidcUser, ctx.auth);

        assertThat(result.monthlyTokensUsed()).isEqualTo(123_456L);
        assertThat(result.monthlyTokensBudget()).isEqualTo(3_000_000L);
    }

    // ─── helpers ────────────────────────────────────────────────────────────

    private record TestContext(User user, Workspace workspace, DefaultOidcUser oidcUser,
                               OAuth2AuthenticationToken auth) {}

    private TestContext buildContext(String role) {
        User u = user(UUID.randomUUID(), "test@example.com");

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(u);

        Workspace ws = new Workspace();
        ws.setId(UUID.randomUUID());

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(ws);
        member.setUser(u);
        member.setMemberRole(role);
        when(memberRepo.findByUserAndPrimaryTrue(u)).thenReturn(Optional.of(member));

        Map<String, Object> claims = Map.of("sub", "google-sub", "email", "test@example.com",
                "iss", "https://accounts.google.com");
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(),
                Instant.now().plusSeconds(3600), claims);
        DefaultOidcUser oidcUser = new DefaultOidcUser(List.of(new OidcUserAuthority(idToken)), idToken, "sub");
        OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(oidcUser, oidcUser.getAuthorities(), "google");

        return new TestContext(u, ws, oidcUser, auth);
    }

    private User user(UUID id, String email) {
        User u = new User();
        u.setId(id);
        u.setEmail(email);
        u.setStatus("ACTIVE");
        return u;
    }

    private CaseFile caseFile(UUID id, Workspace ws, String title) {
        CaseFile cf = new CaseFile();
        cf.setId(id);
        cf.setWorkspace(ws);
        cf.setTitle(title);
        cf.setStatus("OPEN");
        return cf;
    }

    private UsageEvent event(UUID caseFileId, UUID userId, int input, int output, BigDecimal cost) {
        UsageEvent e = new UsageEvent();
        e.setCaseFileId(caseFileId);
        e.setUserId(userId);
        e.setEventType(JobType.CASE_ANALYSIS);
        e.setTokensInput(input);
        e.setTokensOutput(output);
        e.setEstimatedCost(cost);
        return e;
    }
}
