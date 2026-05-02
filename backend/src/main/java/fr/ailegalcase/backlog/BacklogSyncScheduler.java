package fr.ailegalcase.backlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * F-178 SF-178-02 : automatise la sync MD → DB.
 *
 * Deux mécanismes :
 * - {@code @EventListener(ApplicationReadyEvent.class)} → un sync STARTUP
 *   au démarrage pour peupler la DB dès le premier accès à
 *   {@code /super-admin/backlog} après un déploiement.
 * - {@code @Scheduled(cron = "0 *\/5 * * * *")} → un sync SCHEDULED toutes
 *   les 5 minutes UTC pour garder la DB alignée avec les fichiers MD.
 *
 * Toute exception est catch + loggée WARN — ne propage pas pour ne pas
 * désactiver le scheduler. L'audit row est persistée par
 * {@link BacklogSyncService} avant la propagation, donc on garde la trace
 * de l'échec dans {@code backlog_sync_runs}.
 */
@Component
public class BacklogSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(BacklogSyncScheduler.class);

    private final BacklogSyncService syncService;

    public BacklogSyncScheduler(BacklogSyncService syncService) {
        this.syncService = syncService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void syncOnStartup() {
        runSync(SyncTrigger.STARTUP);
    }

    @Scheduled(cron = "0 */5 * * * *")
    public void syncPeriodically() {
        runSync(SyncTrigger.SCHEDULED);
    }

    private void runSync(SyncTrigger trigger) {
        try {
            syncService.sync(trigger);
        } catch (Exception e) {
            log.warn("Backlog sync ({}) failed — audit row persisted, will retry on next cycle: {}",
                    trigger, e.getMessage());
        }
    }
}
