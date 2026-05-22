package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * F-JU-01 / SF-JU-01-02 — déclencheur mensuel du
 * {@link JurisprudenceWatchService}.
 *
 * <p>Activation conditionnée par {@code jurisprudence.watch.enabled} (défaut
 * {@code false}) — empêche tout run involontaire en CI / dev local sans
 * compte OAuth2 PISTE configuré.</p>
 *
 * <p>Cron : 1er du mois à 3h UTC (heure creuse).</p>
 */
@Component
public class JurisprudenceWatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceWatchScheduler.class);

    private final JurisprudenceWatchService service;
    private final boolean enabled;

    public JurisprudenceWatchScheduler(JurisprudenceWatchService service,
                                       @Value("${jurisprudence.watch.enabled:false}") boolean enabled) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(cron = "0 0 3 1 * *", zone = "UTC")
    public void runMonthly() {
        if (!enabled) {
            log.debug("F-JU-01 — JurisprudenceWatchScheduler skipped (enabled=false)");
            return;
        }
        try {
            service.runMonthlyWatch();
        } catch (Exception e) {
            log.error("F-JU-01 — JurisprudenceWatchScheduler unexpected failure", e);
        }
    }
}
