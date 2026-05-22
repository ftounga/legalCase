package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * F-JU-01 / SF-JU-01-03 — déclencheur quotidien du
 * {@link JurisprudenceDriftService}.
 *
 * <p>Activation conditionnée par {@code jurisprudence.drift.enabled} (défaut
 * {@code false}) — sécurité supplémentaire au cas où aucun outil n'a encore
 * implémenté {@link ToolBranchRegistry}.</p>
 */
@Component
public class JurisprudenceDriftScheduler {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceDriftScheduler.class);

    private final JurisprudenceDriftService service;
    private final boolean enabled;

    public JurisprudenceDriftScheduler(JurisprudenceDriftService service,
                                       @Value("${jurisprudence.drift.enabled:false}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    public void runDaily() {
        if (!enabled) {
            log.debug("F-JU-01 — JurisprudenceDriftScheduler skipped (enabled=false)");
            return;
        }
        try {
            service.runDriftScan();
        } catch (Exception e) {
            log.error("F-JU-01 — JurisprudenceDriftScheduler unexpected failure", e);
        }
    }
}
