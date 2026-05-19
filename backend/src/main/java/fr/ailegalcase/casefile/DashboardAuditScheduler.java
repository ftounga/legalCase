package fr.ailegalcase.casefile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * F-180 SF-180-01 — déclenche l'audit dashboard hebdomadaire.
 *
 * <p>{@code @Scheduled(cron = "0 0 8 * * MON", zone = "UTC")} : chaque lundi à
 * 8h UTC, un run d'audit est poussé dans {@code dashboard_audit_runs}. L'endpoint
 * {@code GET /dashboard-audit/latest} lit ce snapshot sans recalculer.</p>
 *
 * <p>Toute exception est catchée + loggée WARN — elle ne propage pas, pour ne
 * pas désactiver le scheduler (même pattern que {@code BacklogSyncScheduler}).</p>
 */
@Component
public class DashboardAuditScheduler {

    private static final Logger log = LoggerFactory.getLogger(DashboardAuditScheduler.class);

    private final DashboardAuditService auditService;

    public DashboardAuditScheduler(DashboardAuditService auditService) {
        this.auditService = auditService;
    }

    @Scheduled(cron = "0 0 8 * * MON", zone = "UTC")
    public void runWeeklyAudit() {
        try {
            auditService.runAudit();
            log.info("F-180 — audit dashboard hebdomadaire exécuté");
        } catch (Exception e) {
            log.warn("F-180 — audit dashboard hebdomadaire en échec, retry au prochain cycle : {}",
                    e.toString());
        }
    }
}
