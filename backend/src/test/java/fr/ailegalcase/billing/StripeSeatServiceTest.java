package fr.ailegalcase.billing;

import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StripeSeatServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;

    // Service en mode stripeEnabled=false → pas d'appel réseau dans les tests unitaires.
    // Les tests IT avec Stripe enabled sont volontairement hors scope (mock Stripe SDK).
    private StripeSeatService service;

    @BeforeEach
    void setUp() {
        service = new StripeSeatService(false, "sk_test_fake",
                subscriptionRepository, workspaceMemberRepository);
    }

    // U-01 : workspace sans subscription → no-op
    @Test
    void syncSeatCount_noSubscription_noop() {
        UUID wid = UUID.randomUUID();
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.empty());

        service.syncSeatCount(wid);

        verify(subscriptionRepository, never()).save(any());
    }

    // U-02 : FREE plan → seatCount DB seulement, pas d'appel Stripe (stripeEnabled=false de toute façon)
    @Test
    void syncSeatCount_freePlan_updatesDbOnly() {
        UUID wid = UUID.randomUUID();
        Subscription sub = buildSub(wid, "FREE", null);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(workspaceMemberRepository.findByWorkspace_Id(wid)).thenReturn(List.of(buildMember(wid)));

        service.syncSeatCount(wid);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getSeatCount()).isEqualTo(1);
    }

    // U-03 : SOLO plan → seatCount DB seulement
    @Test
    void syncSeatCount_soloPlan_updatesDbOnly() {
        UUID wid = UUID.randomUUID();
        Subscription sub = buildSub(wid, "SOLO", "sub_123");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(workspaceMemberRepository.findByWorkspace_Id(wid)).thenReturn(List.of(buildMember(wid)));

        service.syncSeatCount(wid);

        verify(subscriptionRepository).save(any());
        assertThat(sub.getSeatCount()).isEqualTo(1);
    }

    // U-04 : TEAM sans stripeSubscriptionId → no-op Stripe, DB maj
    @Test
    void syncSeatCount_teamNoStripeSubId_updatesDbOnly() {
        UUID wid = UUID.randomUUID();
        Subscription sub = buildSub(wid, "TEAM", null);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(workspaceMemberRepository.findByWorkspace_Id(wid)).thenReturn(List.of(buildMember(wid), buildMember(wid)));

        service.syncSeatCount(wid);

        assertThat(sub.getSeatCount()).isEqualTo(2);
        verify(subscriptionRepository).save(sub);
    }

    // U-05 : workspace avec 0 membre → seatCount minimum 1 (owner toujours compté)
    @Test
    void syncSeatCount_zeroMembers_seatCountAtLeast1() {
        UUID wid = UUID.randomUUID();
        Subscription sub = buildSub(wid, "SOLO", null);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(workspaceMemberRepository.findByWorkspace_Id(wid)).thenReturn(List.of());

        service.syncSeatCount(wid);

        assertThat(sub.getSeatCount()).isEqualTo(1);
    }

    // U-06 : TEAM avec 5 membres → seatCount=5 persisté localement (Stripe disabled = pas d'appel réseau)
    @Test
    void syncSeatCount_teamFiveMembers_seatCountFive() {
        UUID wid = UUID.randomUUID();
        Subscription sub = buildSub(wid, "TEAM", "sub_team");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(workspaceMemberRepository.findByWorkspace_Id(wid)).thenReturn(
                List.of(buildMember(wid), buildMember(wid), buildMember(wid), buildMember(wid), buildMember(wid)));

        service.syncSeatCount(wid);

        assertThat(sub.getSeatCount()).isEqualTo(5);
    }

    private Subscription buildSub(UUID wid, String plan, String stripeSubId) {
        Subscription s = new Subscription();
        s.setWorkspaceId(wid);
        s.setPlanCode(plan);
        s.setStripeSubscriptionId(stripeSubId);
        s.setStatus("ACTIVE");
        s.setSeatCount(1);
        return s;
    }

    private WorkspaceMember buildMember(UUID wid) {
        WorkspaceMember m = new WorkspaceMember();
        Workspace w = new Workspace();
        w.setId(wid);
        m.setWorkspace(w);
        return m;
    }
}
