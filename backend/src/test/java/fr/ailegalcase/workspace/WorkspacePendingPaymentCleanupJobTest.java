package fr.ailegalcase.workspace;

import fr.ailegalcase.billing.Subscription;
import fr.ailegalcase.billing.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SF-156-01 (CA7) — Tests du cleanup horaire des workspaces
 * {@code PENDING_PAYMENT} créés depuis plus de 24 heures.
 */
@ExtendWith(MockitoExtension.class)
class WorkspacePendingPaymentCleanupJobTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private SubscriptionRepository subscriptionRepository;

    private WorkspacePendingPaymentCleanupJob job;

    @BeforeEach
    void setUp() {
        job = new WorkspacePendingPaymentCleanupJob(workspaceRepository,
                workspaceMemberRepository, subscriptionRepository);
    }

    // C-156-01 : workspace PENDING_PAYMENT > 24 h → supprimé (avec sa sub et son owner)
    @Test
    void cleanupOlderThan24h_pendingPayment_deletesCascade() {
        Workspace stale = buildWorkspace(UUID.randomUUID(), WorkspaceStatus.PENDING_PAYMENT);
        when(workspaceRepository.findByStatusAndCreatedAtBefore(eq(WorkspaceStatus.PENDING_PAYMENT), any()))
                .thenReturn(List.of(stale));

        Subscription pendingSub = new Subscription();
        pendingSub.setWorkspaceId(stale.getId());
        when(subscriptionRepository.findByWorkspaceId(stale.getId())).thenReturn(Optional.of(pendingSub));

        WorkspaceMember owner = new WorkspaceMember();
        owner.setWorkspace(stale);
        owner.setMemberRole("OWNER");
        when(workspaceMemberRepository.findByWorkspace_Id(stale.getId())).thenReturn(List.of(owner));

        int deleted = job.cleanupOlderThan(24);

        assertThat(deleted).isEqualTo(1);
        verify(subscriptionRepository).delete(pendingSub);
        verify(workspaceMemberRepository).delete(owner);
        verify(workspaceRepository).delete(stale);
    }

    // C-156-02 : aucun workspace stale → 0 suppression
    @Test
    void cleanupOlderThan24h_noStale_returnsZero() {
        when(workspaceRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of());

        int deleted = job.cleanupOlderThan(24);

        assertThat(deleted).isEqualTo(0);
        verify(workspaceRepository, never()).delete(any());
        verify(subscriptionRepository, never()).delete(any());
    }

    // C-156-03 : le cutoff utilisé est exactement now() - hours.
    //            On vérifie que findByStatusAndCreatedAtBefore reçoit un Instant
    //            cohérent (à +- 5 secondes près).
    @Test
    void cleanupOlderThan_passesCorrectCutoff() {
        when(workspaceRepository.findByStatusAndCreatedAtBefore(any(), any()))
                .thenReturn(List.of());

        Instant before = Instant.now().minus(24, ChronoUnit.HOURS);
        job.cleanupOlderThan(24);
        Instant after = Instant.now().minus(24, ChronoUnit.HOURS);

        var cutoffCaptor = org.mockito.ArgumentCaptor.forClass(Instant.class);
        verify(workspaceRepository).findByStatusAndCreatedAtBefore(
                eq(WorkspaceStatus.PENDING_PAYMENT), cutoffCaptor.capture());
        Instant cutoff = cutoffCaptor.getValue();
        assertThat(cutoff).isAfterOrEqualTo(before.minusSeconds(5));
        assertThat(cutoff).isBeforeOrEqualTo(after.plusSeconds(5));
    }

    private Workspace buildWorkspace(UUID id, String status) {
        Workspace w = new Workspace();
        try {
            var idField = Workspace.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(w, id);
        } catch (Exception e) { throw new RuntimeException(e); }
        w.setName("Stale");
        w.setSlug("slug-" + id);
        w.setPlanCode("TEAM");
        w.setStatus(status);
        w.setLegalDomain("DROIT_DU_TRAVAIL");
        w.setCountry("FRANCE");
        return w;
    }
}
