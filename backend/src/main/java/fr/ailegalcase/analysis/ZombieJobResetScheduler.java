package fr.ailegalcase.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * F-147 SF-147-03 : marque FAILED les jobs {@code analysis_jobs} PROCESSING/PENDING
 * dont {@code updated_at} dépasse le seuil ({@link #STALE_THRESHOLD_MINUTES}).
 *
 * <p>Filet de sécurité : empêche un case file de rester bloqué à cause d'un job
 * zombie (crash pod, exception silencieuse avant SF-147-01, race condition…).
 * Tourne toutes les {@link #RESET_INTERVAL_MS} ms.
 */
@Service
public class ZombieJobResetScheduler {

    private static final Logger log = LoggerFactory.getLogger(ZombieJobResetScheduler.class);

    /** Un job inactif plus de 30 min est considéré zombie. */
    static final int STALE_THRESHOLD_MINUTES = 30;

    /** Exécution toutes les 5 minutes. */
    static final long RESET_INTERVAL_MS = 5 * 60 * 1000L;

    private final AnalysisJobRepository analysisJobRepository;

    public ZombieJobResetScheduler(AnalysisJobRepository analysisJobRepository) {
        this.analysisJobRepository = analysisJobRepository;
    }

    @Scheduled(fixedDelay = RESET_INTERVAL_MS, initialDelay = RESET_INTERVAL_MS)
    @Transactional
    public void resetZombies() {
        Instant now = Instant.now();
        Instant staleBefore = now.minus(STALE_THRESHOLD_MINUTES, ChronoUnit.MINUTES);
        int updated = analysisJobRepository.forceFailZombieJobs(
                staleBefore, "Zombie reset (F-147-03)", now);
        if (updated > 0) {
            log.warn("Zombie reset — {} job(s) older than {} min forced to FAILED",
                    updated, STALE_THRESHOLD_MINUTES);
        }
    }
}
