package fr.ailegalcase.workspace;

import fr.ailegalcase.billing.SubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * SF-156-01 — Invariant SF-156-00 §2 : nettoyage automatique des workspaces
 * créés en {@code PENDING_PAYMENT} et non confirmés sous 24 h.
 *
 * <p>Un avocat peut abandonner la page Stripe puis revenir le lendemain pour
 * payer — 24 h couvre ce cas tout en évitant l'accumulation indéfinie de
 * workspaces zombies si le paiement ne vient jamais.
 *
 * <p>Stratégie de cleanup :
 * <ol>
 *   <li>Marquer le workspace en {@code CANCELLED} (audit visuel court).</li>
 *   <li>Supprimer l'enregistrement {@code Subscription} pendant.</li>
 *   <li>Supprimer le {@code WorkspaceMember} OWNER lié.</li>
 *   <li>Supprimer le workspace définitivement.</li>
 * </ol>
 *
 * <p>Cadence : toutes les heures (cron {@code 0 0 * * * *}).
 */
@Component
public class WorkspacePendingPaymentCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(WorkspacePendingPaymentCleanupJob.class);

    private static final long EXPIRATION_HOURS = 24;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SubscriptionRepository subscriptionRepository;

    public WorkspacePendingPaymentCleanupJob(WorkspaceRepository workspaceRepository,
                                             WorkspaceMemberRepository workspaceMemberRepository,
                                             SubscriptionRepository subscriptionRepository) {
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Cron horaire — supprime tous les workspaces en {@code PENDING_PAYMENT}
     * créés depuis plus de {@value #EXPIRATION_HOURS} heures.
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void cleanupStalePendingPaymentWorkspaces() {
        cleanupOlderThan(EXPIRATION_HOURS);
    }

    /**
     * Exposé pour les tests unitaires — permet de driver la durée de référence.
     *
     * @return nombre de workspaces supprimés
     */
    @Transactional
    public int cleanupOlderThan(long hours) {
        Instant cutoff = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<Workspace> stale = workspaceRepository
                .findByStatusAndCreatedAtBefore(WorkspaceStatus.PENDING_PAYMENT, cutoff);
        if (stale.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        for (Workspace ws : stale) {
            try {
                deleteWorkspaceCascade(ws);
                deleted++;
            } catch (Exception e) {
                log.error("Failed to cleanup stale PENDING_PAYMENT workspace {}: {}",
                        ws.getId(), e.getMessage(), e);
            }
        }
        if (deleted > 0) {
            log.info("Cleanup PENDING_PAYMENT : {} workspace(s) supprimé(s) (cutoff {}h)", deleted, hours);
        }
        return deleted;
    }

    private void deleteWorkspaceCascade(Workspace workspace) {
        UUID workspaceId = workspace.getId();

        // Audit : marquer CANCELLED brièvement avant suppression (utile si
        // un autre process consulte simultanément).
        workspace.setStatus(WorkspaceStatus.CANCELLED);
        workspaceRepository.save(workspace);

        subscriptionRepository.findByWorkspaceId(workspaceId)
                .ifPresent(subscriptionRepository::delete);

        workspaceMemberRepository.findByWorkspace_Id(workspaceId)
                .forEach(workspaceMemberRepository::delete);

        workspaceRepository.delete(workspace);
    }
}
