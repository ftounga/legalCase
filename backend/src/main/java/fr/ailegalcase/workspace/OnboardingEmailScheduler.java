package fr.ailegalcase.workspace;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class OnboardingEmailScheduler {

    private static final Logger log = LoggerFactory.getLogger(OnboardingEmailScheduler.class);

    private final UserRepository userRepository;
    private final EmailService emailService;

    public OnboardingEmailScheduler(UserRepository userRepository, EmailService emailService) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void sendDailyOnboardingEmails() {
        Instant now = Instant.now();

        sendForDay(now, 2, "J+2");
        sendForDay(now, 5, "J+5");
        sendForDay(now, 12, "J+12");
        sendForDay(now, 15, "J+15");
    }

    private void sendForDay(Instant now, int daysAgo, String label) {
        Instant dayStart = now.truncatedTo(ChronoUnit.DAYS).minus(daysAgo, ChronoUnit.DAYS);
        Instant dayEnd = dayStart.plus(1, ChronoUnit.DAYS);

        List<User> users = userRepository.findByCreatedAtBetween(dayStart, dayEnd);
        if (users.isEmpty()) {
            log.debug("OnboardingScheduler {}: no users registered {} days ago", label, daysAgo);
            return;
        }

        log.info("OnboardingScheduler {}: {} user(s) to notify", label, users.size());

        for (User user : users) {
            try {
                switch (daysAgo) {
                    case 2  -> emailService.sendOnboardingTipAnalysis(user);
                    case 5  -> emailService.sendOnboardingTipShare(user);
                    case 12 -> emailService.sendOnboardingBeforeExpiry(user);
                    case 15 -> emailService.sendOnboardingExpired(user);
                    default -> log.warn("OnboardingScheduler: unknown daysAgo={}", daysAgo);
                }
            } catch (Exception e) {
                log.warn("OnboardingScheduler {}: failed for user {} — {}", label, user.getId(), e.getMessage());
            }
        }
    }
}
