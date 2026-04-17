package fr.ailegalcase.timetracking.service;

import fr.ailegalcase.timetracking.entity.TimeEntry;
import fr.ailegalcase.timetracking.repository.TimeEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * SF-106-06 : cron qui détecte et arrête automatiquement les timers
 * actifs depuis plus de 8h (oubli de l'utilisateur, session expirée,
 * déconnexion sans arrêt du chrono).
 */
@Service
public class TimerAutoStopScheduler {

    private static final Logger log = LoggerFactory.getLogger(TimerAutoStopScheduler.class);
    private static final int MAX_DURATION_SECONDS = 8 * 3600;

    private final TimeEntryRepository timeEntryRepository;

    public TimerAutoStopScheduler(TimeEntryRepository timeEntryRepository) {
        this.timeEntryRepository = timeEntryRepository;
    }

    @Scheduled(fixedRate = 3600_000)
    @Transactional
    public void autoStopStaleTimers() {
        Instant cutoff = Instant.now().minusSeconds(MAX_DURATION_SECONDS);
        List<TimeEntry> stale = timeEntryRepository.findStaleTimers(cutoff);
        if (stale.isEmpty()) return;

        Instant now = Instant.now();
        for (TimeEntry entry : stale) {
            entry.setStoppedAt(now);
            entry.setDurationSeconds(MAX_DURATION_SECONDS);
            timeEntryRepository.save(entry);
        }
        log.info("Auto-stopped {} stale timer(s) (started before {})", stale.size(), cutoff);
    }
}
