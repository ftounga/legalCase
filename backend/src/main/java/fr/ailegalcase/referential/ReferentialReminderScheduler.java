package fr.ailegalcase.referential;

import fr.ailegalcase.workspace.EmailService;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Daily cron that sends a reminder email to all OWNERs for alerts pending more than 15 days.
 */
@Component
public class ReferentialReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReferentialReminderScheduler.class);
    private static final int REMINDER_DAYS = 15;

    private final ReferentialAlertRepository alertRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final EmailService emailService;
    private final String frontendUrl;

    public ReferentialReminderScheduler(
            ReferentialAlertRepository alertRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            EmailService emailService,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.alertRepository = alertRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendReminders() {
        Instant threshold = Instant.now().minus(REMINDER_DAYS, ChronoUnit.DAYS);
        List<ReferentialAlert> overdue = alertRepository.findPendingWithoutReminderBefore(threshold);
        if (overdue.isEmpty()) {
            return;
        }
        log.info("ReferentialReminderScheduler: {} alerts overdue for reminder", overdue.size());

        List<WorkspaceMember> owners = workspaceMemberRepository.findByMemberRole("OWNER");
        owners.forEach(owner -> {
            try {
                emailService.sendReferentialAlertReminder(owner.getUser().getEmail(), frontendUrl);
            } catch (Exception e) {
                log.warn("ReferentialReminderScheduler: failed to notify {} — {}", owner.getUser().getEmail(), e.getMessage());
            }
        });

        overdue.forEach(a -> a.setReminderSentAt(Instant.now()));
        alertRepository.saveAll(overdue);
    }
}
