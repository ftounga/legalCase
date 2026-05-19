package fr.ailegalcase.billing;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingCancellationServiceTest {

    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;
    @Mock private StripeCustomerService stripeCustomerService;
    @Mock private OidcUser oidcUser;

    private BillingCancellationService service;

    @BeforeEach
    void setUp() {
        service = new BillingCancellationService(subscriptionRepository,
                workspaceMemberRepository, currentUserResolver, stripeCustomerService);
    }

    // U-01 : cancel nominal — programme la résiliation, persiste cancelAtPeriodEnd=true
    @Test
    void cancel_ownerPaidWorkspace_schedulesCancellation() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "OWNER");
        Subscription sub = paidSubscription(wid);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getCurrentPeriodEnd()).thenReturn(1_900_000_000L);
        when(stripeCustomerService.scheduleCancellation("sub_123")).thenReturn(stripeSub);

        SubscriptionStateResponse r = service.cancel(oidcUser, "google", null);

        assertThat(r.cancelAtPeriodEnd()).isTrue();
        assertThat(r.planCode()).isEqualTo("SOLO");
        assertThat(r.currentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1_900_000_000L));
        assertThat(sub.isCancelAtPeriodEnd()).isTrue();
        verify(subscriptionRepository).save(sub);
    }

    // U-02 : cancel par non-OWNER → 403, pas d'appel Stripe
    @Test
    void cancel_nonOwner_throws403() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "ADMIN");

        assertThatThrownBy(() -> service.cancel(oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
        verifyNoInteractions(stripeCustomerService);
        verify(subscriptionRepository, never()).save(any());
    }

    // U-03 : cancel sur workspace FREE → 409
    @Test
    void cancel_freeWorkspace_throws409() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "OWNER");
        Subscription sub = new Subscription();
        sub.setPlanCode("FREE");
        sub.setStatus("ACTIVE");
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.cancel(oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verifyNoInteractions(stripeCustomerService);
    }

    // U-04 : cancel alors qu'une résiliation est déjà programmée → 409
    @Test
    void cancel_alreadyScheduled_throws409() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "OWNER");
        Subscription sub = paidSubscription(wid);
        sub.setCancelAtPeriodEnd(true);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.cancel(oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verifyNoInteractions(stripeCustomerService);
    }

    // U-05 : échec Stripe → 502
    @Test
    void cancel_stripeFailure_throws502() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "OWNER");
        Subscription sub = paidSubscription(wid);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(stripeCustomerService.scheduleCancellation("sub_123"))
                .thenThrow(new RuntimeException("Stripe cancellation update failed"));

        assertThatThrownBy(() -> service.cancel(oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("502");
        verify(subscriptionRepository, never()).save(any());
    }

    // U-06 : Stripe désactivé (scheduleCancellation renvoie null) → 409, état local inchangé
    @Test
    void cancel_stripeDisabled_throws409AndDoesNotChangeLocalState() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "OWNER");
        Subscription sub = paidSubscription(wid);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(stripeCustomerService.scheduleCancellation("sub_123")).thenReturn(null);

        assertThatThrownBy(() -> service.cancel(oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(sub.isCancelAtPeriodEnd()).isFalse();
        verify(subscriptionRepository, never()).save(any());
    }

    // U-07 : resume nominal — annule la résiliation programmée
    @Test
    void resume_scheduledCancellation_resumesSubscription() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "OWNER");
        Subscription sub = paidSubscription(wid);
        sub.setCancelAtPeriodEnd(true);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeSub.getCurrentPeriodEnd()).thenReturn(1_900_000_000L);
        when(stripeCustomerService.resumeSubscription("sub_123")).thenReturn(stripeSub);

        SubscriptionStateResponse r = service.resume(oidcUser, "google", null);

        assertThat(r.cancelAtPeriodEnd()).isFalse();
        assertThat(sub.isCancelAtPeriodEnd()).isFalse();
        verify(subscriptionRepository).save(sub);
    }

    // U-08 : resume sans résiliation programmée → 409
    @Test
    void resume_noScheduledCancellation_throws409() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "OWNER");
        Subscription sub = paidSubscription(wid);
        sub.setCancelAtPeriodEnd(false);
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service.resume(oidcUser, "google", null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        verifyNoInteractions(stripeCustomerService);
    }

    // U-09 : getState — tout membre du workspace lit l'état courant
    @Test
    void getState_anyMember_returnsCurrentState() {
        UUID wid = UUID.randomUUID();
        setupMember(wid, "LAWYER");
        Subscription sub = paidSubscription(wid);
        sub.setCancelAtPeriodEnd(true);
        sub.setCurrentPeriodEnd(Instant.ofEpochSecond(1_900_000_000L));
        when(subscriptionRepository.findByWorkspaceId(wid)).thenReturn(Optional.of(sub));

        SubscriptionStateResponse r = service.getState(oidcUser, "google", null);

        assertThat(r.planCode()).isEqualTo("SOLO");
        assertThat(r.status()).isEqualTo("ACTIVE");
        assertThat(r.cancelAtPeriodEnd()).isTrue();
        assertThat(r.currentPeriodEnd()).isEqualTo(Instant.ofEpochSecond(1_900_000_000L));
    }

    // U-10 : isolation workspace — le service n'opère que sur la Subscription du
    // workspace courant résolu via le membre primary de l'utilisateur.
    @Test
    void cancel_operatesOnlyOnCurrentWorkspaceSubscription() {
        UUID currentWid = UUID.randomUUID();
        setupMember(currentWid, "OWNER");
        Subscription sub = paidSubscription(currentWid);
        when(subscriptionRepository.findByWorkspaceId(currentWid)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        com.stripe.model.Subscription stripeSub = mock(com.stripe.model.Subscription.class);
        when(stripeCustomerService.scheduleCancellation("sub_123")).thenReturn(stripeSub);

        service.cancel(oidcUser, "google", null);

        // Le repository n'est interrogé que pour le workspace courant.
        verify(subscriptionRepository).findByWorkspaceId(currentWid);
        verify(subscriptionRepository, never()).findByWorkspaceId(
                org.mockito.ArgumentMatchers.argThat(id -> !currentWid.equals(id)));
    }

    private void setupMember(UUID workspaceId, String role) {
        User u = new User();
        u.setId(UUID.randomUUID());
        Workspace w = new Workspace();
        w.setId(workspaceId);
        WorkspaceMember member = new WorkspaceMember();
        member.setUser(u);
        member.setWorkspace(w);
        member.setMemberRole(role);
        member.setPrimary(true);
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(u);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(u)).thenReturn(Optional.of(member));
    }

    private Subscription paidSubscription(UUID workspaceId) {
        Subscription sub = new Subscription();
        sub.setWorkspaceId(workspaceId);
        sub.setPlanCode("SOLO");
        sub.setStatus("ACTIVE");
        sub.setStripeSubscriptionId("sub_123");
        return sub;
    }
}
