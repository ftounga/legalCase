package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.AuthAccount;
import fr.ailegalcase.auth.AuthAccountRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.billing.StripeCheckoutService;
import fr.ailegalcase.billing.StripeCustomerService;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SF-156-01 — Tests unitaires de la création d'un workspace supplémentaire :
 * gate plan TEAM/PRO, état PENDING_PAYMENT, rollback transactionnel sur
 * échec Stripe.
 */
@ExtendWith(MockitoExtension.class)
class WorkspaceServiceCreateSf15601Test {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private AuthAccountRepository authAccountRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private StripeCustomerService stripeCustomerService;
    @Mock private StripeCheckoutService stripeCheckoutService;
    @Mock private PlanLimitService planLimitService;
    @Mock private EmailService emailService;

    private WorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceService(workspaceRepository, workspaceMemberRepository,
                authAccountRepository, subscriptionRepository, stripeCustomerService,
                stripeCheckoutService, planLimitService, emailService);
    }

    // U-156-01 : OWNER FREE qui a déjà un workspace → 403 sur création workspace supplémentaire
    @Test
    void createWorkspace_ownerFree_returns403() {
        User user = userWithMembership("FREE", "OWNER", true);

        assertThatThrownBy(() -> service.createWorkspace(null, "LOCAL",
                "Cabinet Bordeaux", "DROIT_DU_TRAVAIL", "FRANCE", "TEAM", () -> "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));

        verify(workspaceRepository, never()).save(any());
    }

    // U-156-02 : OWNER SOLO → 403
    @Test
    void createWorkspace_ownerSolo_returns403() {
        User user = userWithMembership("SOLO", "OWNER", true);

        assertThatThrownBy(() -> service.createWorkspace(null, "LOCAL",
                "Cabinet Bordeaux", "DROIT_DU_TRAVAIL", "FRANCE", "TEAM", () -> "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // U-156-03 : OWNER TEAM → workspace créé en PENDING_PAYMENT + sub PENDING_PAYMENT + URL Stripe
    @Test
    void createWorkspace_ownerTeam_createsPendingPayment() {
        User user = userWithMembership("TEAM", "OWNER", true);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> {
            Workspace w = inv.getArgument(0);
            // Simule la génération d'ID en BDD pour permettre l'appel Stripe.
            if (w.getId() == null) {
                try {
                    var idField = Workspace.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(w, UUID.randomUUID());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            return w;
        });
        when(stripeCheckoutService.createSubscriptionSessionForNewWorkspace(
                eq("TEAM"), eq("owner@example.com"), any()))
                .thenReturn(Optional.of("https://checkout.stripe.com/c/pay/cs_test_xyz"));

        WorkspaceCreatedResponse response = service.createWorkspace(null, "LOCAL",
                "Cabinet Bordeaux", "DROIT_DU_TRAVAIL", "FRANCE", "TEAM", () -> "owner@example.com");

        assertThat(response.status()).isEqualTo(WorkspaceStatus.PENDING_PAYMENT);
        assertThat(response.planCode()).isEqualTo("TEAM");
        assertThat(response.stripeCheckoutUrl()).startsWith("https://checkout.stripe.com");

        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository, atLeastOnce()).save(workspaceCaptor.capture());
        Workspace created = workspaceCaptor.getValue();
        assertThat(created.getStatus()).isEqualTo(WorkspaceStatus.PENDING_PAYMENT);
        assertThat(created.getPlanCode()).isEqualTo("TEAM");

        ArgumentCaptor<Subscription> subCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, atLeastOnce()).save(subCaptor.capture());
        Subscription sub = subCaptor.getValue();
        assertThat(sub.getStatus()).isEqualTo("PENDING_PAYMENT");
        assertThat(sub.getPlanCode()).isEqualTo("TEAM");

        // CA9 : nouveau workspace jamais marqué primary (l'OWNER reste sur son
        // workspace courant — isolation).
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().isPrimary()).isFalse();
    }

    // U-156-04 : OWNER PRO → idem mais plan PRO
    @Test
    void createWorkspace_ownerPro_createsPendingPaymentPro() {
        User user = userWithMembership("PRO", "OWNER", true);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> {
            Workspace w = inv.getArgument(0);
            if (w.getId() == null) {
                try {
                    var idField = Workspace.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(w, UUID.randomUUID());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            return w;
        });
        when(stripeCheckoutService.createSubscriptionSessionForNewWorkspace(
                eq("PRO"), any(), any()))
                .thenReturn(Optional.of("https://checkout.stripe.com/c/pay/cs_test_pro"));

        WorkspaceCreatedResponse response = service.createWorkspace(null, "LOCAL",
                "Cabinet Lyon", "DROIT_DU_TRAVAIL", "FRANCE", "PRO", () -> "owner@example.com");

        assertThat(response.planCode()).isEqualTo("PRO");
        assertThat(response.status()).isEqualTo(WorkspaceStatus.PENDING_PAYMENT);
    }

    // U-156-05 : plan invalide (FREE / SOLO / null / INVALID) → 400
    @Test
    void createWorkspace_invalidPlan_returns400_free() {
        User user = userWithMembership("TEAM", "OWNER", true);
        assertThatThrownBy(() -> service.createWorkspace(null, "LOCAL",
                "Cabinet X", "DROIT_DU_TRAVAIL", "FRANCE", "FREE", () -> "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createWorkspace_invalidPlan_returns400_solo() {
        User user = userWithMembership("TEAM", "OWNER", true);
        assertThatThrownBy(() -> service.createWorkspace(null, "LOCAL",
                "Cabinet X", "DROIT_DU_TRAVAIL", "FRANCE", "SOLO", () -> "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void createWorkspace_invalidPlan_returns400_null() {
        User user = userWithMembership("TEAM", "OWNER", true);
        assertThatThrownBy(() -> service.createWorkspace(null, "LOCAL",
                "Cabinet X", "DROIT_DU_TRAVAIL", "FRANCE", null, () -> "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    // U-156-06 : Stripe échoue → 502 + rollback transactionnel (la transaction
    // du service va rollback ; on vérifie qu'aucun appel save non-précédé
    // d'un Stripe success n'est commit — mais c'est le rôle du framework,
    // on vérifie ici que l'exception remonte bien.)
    @Test
    void createWorkspace_stripeFailure_propagates502() {
        User user = userWithMembership("TEAM", "OWNER", true);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> {
            Workspace w = inv.getArgument(0);
            if (w.getId() == null) {
                try {
                    var idField = Workspace.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(w, UUID.randomUUID());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            return w;
        });
        when(stripeCheckoutService.createSubscriptionSessionForNewWorkspace(any(), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "Service de paiement indisponible"));

        assertThatThrownBy(() -> service.createWorkspace(null, "LOCAL",
                "Cabinet X", "DROIT_DU_TRAVAIL", "FRANCE", "TEAM", () -> "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    // U-156-07 : MEMBER (pas OWNER) tente → 403
    @Test
    void createWorkspace_userIsMemberNotOwner_returns403() {
        User user = userWithMembership("TEAM", "MEMBER", true);

        assertThatThrownBy(() -> service.createWorkspace(null, "LOCAL",
                "Cabinet X", "DROIT_DU_TRAVAIL", "FRANCE", "TEAM", () -> "owner@example.com"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.FORBIDDEN));
    }

    // U-156-08 : Stripe désactivé (env dev) → workspace créé ACTIVE
    // directement, pas d'URL retournée.
    @Test
    void createWorkspace_stripeDisabled_activatesImmediately() {
        User user = userWithMembership("TEAM", "OWNER", true);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> {
            Workspace w = inv.getArgument(0);
            if (w.getId() == null) {
                try {
                    var idField = Workspace.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(w, UUID.randomUUID());
                } catch (Exception e) { throw new RuntimeException(e); }
            }
            return w;
        });
        when(stripeCheckoutService.createSubscriptionSessionForNewWorkspace(any(), any(), any()))
                .thenReturn(Optional.empty());

        WorkspaceCreatedResponse response = service.createWorkspace(null, "LOCAL",
                "Cabinet X", "DROIT_DU_TRAVAIL", "FRANCE", "TEAM", () -> "owner@example.com");

        assertThat(response.status()).isEqualTo(WorkspaceStatus.ACTIVE);
        assertThat(response.stripeCheckoutUrl()).isNull();
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private User userWithMembership(String currentPlan, String role, boolean stubAuth) {
        User user = new User();
        user.setEmail("owner@example.com");
        try {
            var idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }

        if (stubAuth) {
            AuthAccount account = new AuthAccount();
            account.setUser(user);
            account.setProvider("LOCAL");
            account.setProviderUserId("owner@example.com");
            when(authAccountRepository.findByProviderAndProviderUserId("LOCAL", "owner@example.com"))
                    .thenReturn(Optional.of(account));
        }

        // L'utilisateur a déjà un workspace : isFirstWorkspace = false
        when(workspaceMemberRepository.existsByUser(user)).thenReturn(true);

        // Workspace courant.
        Workspace currentWs = new Workspace();
        try {
            var idField = Workspace.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(currentWs, UUID.randomUUID());
        } catch (Exception e) { throw new RuntimeException(e); }
        currentWs.setName("Current");
        currentWs.setSlug("slug-current");
        currentWs.setOwner(user);
        currentWs.setPlanCode(currentPlan);
        currentWs.setStatus(WorkspaceStatus.ACTIVE);
        currentWs.setLegalDomain("DROIT_DU_TRAVAIL");
        currentWs.setCountry("FRANCE");

        WorkspaceMember currentMember = new WorkspaceMember();
        currentMember.setWorkspace(currentWs);
        currentMember.setUser(user);
        currentMember.setMemberRole(role);
        currentMember.setPrimary(true);

        lenient().when(workspaceMemberRepository.findByUserAndPrimaryTrue(user))
                .thenReturn(Optional.of(currentMember));
        lenient().when(workspaceMemberRepository.findByUser(user))
                .thenReturn(List.of(currentMember));
        lenient().when(planLimitService.getPlanCodeForWorkspace(currentWs.getId()))
                .thenReturn(currentPlan);

        return user;
    }
}
