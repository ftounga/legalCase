package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.billing.StripeCustomerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private WorkspaceMemberRepository workspaceMemberRepository;

    @Mock
    private fr.ailegalcase.auth.AuthAccountRepository authAccountRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private StripeCustomerService stripeCustomerService;

    private WorkspaceService service;

    @BeforeEach
    void setUp() {
        service = new WorkspaceService(workspaceRepository, workspaceMemberRepository,
                authAccountRepository, subscriptionRepository, stripeCustomerService);
    }

    // U-01 : premier login → workspace FREE + expires_at 14j + membre OWNER créés
    @Test
    void createDefaultWorkspace_firstLogin_createsWorkspaceAndOwnerMember() {
        User user = new User();
        user.setEmail("john@example.com");

        when(workspaceMemberRepository.existsByUser(user)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeCustomerService.createCustomer(any(), any())).thenReturn(Optional.empty());

        service.createDefaultWorkspace(user);

        ArgumentCaptor<Workspace> workspaceCaptor = ArgumentCaptor.forClass(Workspace.class);
        verify(workspaceRepository).save(workspaceCaptor.capture());
        assertThat(workspaceCaptor.getValue().getName()).isEqualTo("john@example.com");
        assertThat(workspaceCaptor.getValue().getPlanCode()).isEqualTo("FREE");
        assertThat(workspaceCaptor.getValue().getStatus()).isEqualTo("ACTIVE");
        assertThat(workspaceCaptor.getValue().getSlug()).isNotBlank();

        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getMemberRole()).isEqualTo("OWNER");
        assertThat(memberCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(memberCaptor.getValue().isPrimary()).isTrue();

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, atLeastOnce()).save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getAllValues().get(0);
        assertThat(saved.getPlanCode()).isEqualTo("FREE");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getStartedAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isNotNull();
        assertThat(saved.getExpiresAt()).isCloseTo(
                Instant.now().plus(14, ChronoUnit.DAYS), within(5, ChronoUnit.SECONDS));
    }

    // U-01b : Stripe disponible → stripe_customer_id persisté
    @Test
    void createDefaultWorkspace_stripeAvailable_savesCustomerId() {
        User user = new User();
        user.setEmail("john@example.com");

        when(workspaceMemberRepository.existsByUser(user)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeCustomerService.createCustomer(any(), any())).thenReturn(Optional.of("cus_test123"));

        service.createDefaultWorkspace(user);

        ArgumentCaptor<Subscription> subscriptionCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, atLeast(2)).save(subscriptionCaptor.capture());
        Subscription lastSave = subscriptionCaptor.getAllValues().get(subscriptionCaptor.getAllValues().size() - 1);
        assertThat(lastSave.getStripeCustomerId()).isEqualTo("cus_test123");
    }

    // U-01c : Stripe en erreur (fail-open) → workspace créé, stripe_customer_id null
    @Test
    void createDefaultWorkspace_stripeFails_workspaceCreatedWithoutCustomerId() {
        User user = new User();
        user.setEmail("john@example.com");

        when(workspaceMemberRepository.existsByUser(user)).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenAnswer(inv -> inv.getArgument(0));
        when(stripeCustomerService.createCustomer(any(), any())).thenReturn(Optional.empty());

        service.createDefaultWorkspace(user);

        verify(workspaceRepository).save(any(Workspace.class));
        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getStripeCustomerId()).isNull();
    }

    // U-02 : login suivant (workspace existant) → aucune création
    @Test
    void createDefaultWorkspace_existingWorkspace_doesNotCreate() {
        User user = new User();
        user.setEmail("john@example.com");

        when(workspaceMemberRepository.existsByUser(user)).thenReturn(true);

        service.createDefaultWorkspace(user);

        verify(workspaceRepository, never()).save(any());
        verify(workspaceMemberRepository, never()).save(any());
        verify(subscriptionRepository, never()).save(any());
    }
}
