package fr.ailegalcase.billing;

import fr.ailegalcase.analysis.UsageEventRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.chat.ChatMessageRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import fr.ailegalcase.workspace.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingSeatsServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private UsageEventRepository usageEventRepository;
    @Mock private ChatMessageRepository chatMessageRepository;
    @Mock private CreditPurchaseService creditPurchaseService;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private OidcUser oidcUser;

    private BillingSeatsService service;
    private PlanLimitService planLimitService;

    @BeforeEach
    void setUp() {
        planLimitService = new PlanLimitService(subscriptionRepository, usageEventRepository,
                chatMessageRepository, creditPurchaseService, workspaceRepository);
        service = new BillingSeatsService(planLimitService, workspaceMemberRepository, currentUserResolver);
    }

    // U-01 : TEAM 4 seats (3 inclus + 1 supp) → 219 + 59 = 278 €
    @Test
    void getSummary_team4Seats_returns278euros() {
        UUID wid = UUID.randomUUID();
        setupOwnerWith(wid, "TEAM", 4);

        SeatsSummaryResponse r = service.getSummary(oidcUser, "google", null);

        assertThat(r.planCode()).isEqualTo("TEAM");
        assertThat(r.seatCount()).isEqualTo(4);
        assertThat(r.includedSeats()).isEqualTo(3);
        assertThat(r.maxSeats()).isEqualTo(6);
        assertThat(r.extraSeatPriceCents()).isEqualTo(5900);
        assertThat(r.baseMonthlyCostCents()).isEqualTo(21900);
        assertThat(r.totalMonthlyCostCents()).isEqualTo(27800); // 219 + 59
    }

    // U-02 : SOLO 1 seat → 99 € pur, extra = 0
    @Test
    void getSummary_solo1Seat_returns99euros() {
        UUID wid = UUID.randomUUID();
        setupOwnerWith(wid, "SOLO", 1);

        SeatsSummaryResponse r = service.getSummary(oidcUser, "google", null);

        assertThat(r.totalMonthlyCostCents()).isEqualTo(9900);
        assertThat(r.includedSeats()).isEqualTo(1);
        assertThat(r.maxSeats()).isEqualTo(1);
        assertThat(r.extraSeatPriceCents()).isEqualTo(0);
    }

    // U-03 : FREE → 0 € total, 1 seat, max 1
    @Test
    void getSummary_free_returns0euros() {
        UUID wid = UUID.randomUUID();
        setupOwnerWith(wid, "FREE", 1);

        SeatsSummaryResponse r = service.getSummary(oidcUser, "google", null);

        assertThat(r.totalMonthlyCostCents()).isEqualTo(0);
        assertThat(r.maxSeats()).isEqualTo(1);
    }

    // U-04 : PRO 8 seats (5 inclus + 3 supp) → 429 + 3×79 = 666 €
    @Test
    void getSummary_pro8Seats_returns666euros() {
        UUID wid = UUID.randomUUID();
        setupOwnerWith(wid, "PRO", 8);

        SeatsSummaryResponse r = service.getSummary(oidcUser, "google", null);

        assertThat(r.seatCount()).isEqualTo(8);
        assertThat(r.totalMonthlyCostCents()).isEqualTo(66600); // 429 + 3×79
        assertThat(r.maxSeats()).isEqualTo(Integer.MAX_VALUE);
    }

    // U-05 : LAWYER appelle → 403
    @Test
    void getSummary_lawyerCaller_throws403() {
        UUID wid = UUID.randomUUID();
        User u = new User();
        u.setId(UUID.randomUUID());
        Workspace w = new Workspace();
        w.setId(wid);
        WorkspaceMember m = new WorkspaceMember();
        m.setUser(u);
        m.setWorkspace(w);
        m.setMemberRole("LAWYER");

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(u);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(u)).thenReturn(Optional.of(m));

        assertThatThrownBy(() -> service.getSummary(oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    private void setupOwnerWith(UUID workspaceId, String planCode, int memberCount) {
        User u = new User();
        u.setId(UUID.randomUUID());
        Workspace w = new Workspace();
        w.setId(workspaceId);
        WorkspaceMember owner = new WorkspaceMember();
        owner.setUser(u);
        owner.setWorkspace(w);
        owner.setMemberRole("OWNER");

        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(u);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(u)).thenReturn(Optional.of(owner));

        java.util.List<WorkspaceMember> members = new java.util.ArrayList<>();
        for (int i = 0; i < memberCount; i++) members.add(owner);
        when(workspaceMemberRepository.findByWorkspace_Id(workspaceId)).thenReturn(members);

        Subscription sub = new Subscription();
        sub.setPlanCode(planCode);
        when(subscriptionRepository.findByWorkspaceId(workspaceId)).thenReturn(Optional.of(sub));
    }
}
