package fr.ailegalcase.casefile;

import fr.ailegalcase.notification.InAppNotificationService;
import fr.ailegalcase.notification.NotificationType;
import fr.ailegalcase.workspace.EmailService;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class DeadlineAlertService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineAlertService.class);
    private static final int[] ALERT_DAYS = {15, 7};

    private final CaseDeadlineRepository deadlineRepository;
    private final DeadlineAlertSendRepository alertSendRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final EmailService emailService;
    private final InAppNotificationService inAppNotificationService;

    public DeadlineAlertService(CaseDeadlineRepository deadlineRepository,
                                DeadlineAlertSendRepository alertSendRepository,
                                WorkspaceMemberRepository workspaceMemberRepository,
                                EmailService emailService,
                                InAppNotificationService inAppNotificationService) {
        this.deadlineRepository = deadlineRepository;
        this.alertSendRepository = alertSendRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.emailService = emailService;
        this.inAppNotificationService = inAppNotificationService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendDailyAlerts() {
        LocalDate today = LocalDate.now();

        sendStandardAlerts(today);
        sendMultiThresholdAlerts(today);
    }

    void sendStandardAlerts(LocalDate today) {
        List<LocalDate> targetDates = List.of(today.plusDays(15), today.plusDays(7));

        List<CaseDeadline> deadlines = deadlineRepository
                .findByDueDateInAndCaseFileDeletedAtIsNull(targetDates);

        if (deadlines.isEmpty()) {
            log.debug("DeadlineAlert: no standard deadlines at J-15/J-7 today ({})", today);
            return;
        }

        log.info("DeadlineAlert: {} standard deadline(s) to notify for {}", deadlines.size(), today);

        for (CaseDeadline deadline : deadlines) {
            if (deadline.getAlertThresholds() != null) continue; // handled by multi-threshold path
            try {
                processDeadline(deadline, today);
            } catch (Exception e) {
                log.error("DeadlineAlert: unexpected error for deadline {} — {}", deadline.getId(), e.getMessage());
            }
        }
    }

    void sendMultiThresholdAlerts(LocalDate today) {
        List<CaseDeadline> deadlines = deadlineRepository
                .findByAlertThresholdsIsNotNullAndCaseFileDeletedAtIsNull();

        if (deadlines.isEmpty()) {
            log.debug("DeadlineAlert: no multi-threshold deadlines today ({})", today);
            return;
        }

        for (CaseDeadline deadline : deadlines) {
            try {
                processMultiThresholdDeadline(deadline, today);
            } catch (Exception e) {
                log.error("DeadlineAlert: unexpected error for multi-threshold deadline {} — {}",
                        deadline.getId(), e.getMessage());
            }
        }
    }

    private void processMultiThresholdDeadline(CaseDeadline deadline, LocalDate today) {
        int[] thresholds = parseThresholds(deadline.getAlertThresholds());
        long daysUntilDue = deadline.getDueDate().toEpochDay() - today.toEpochDay();

        for (int threshold : thresholds) {
            if (daysUntilDue != threshold) continue;
            if (alertSendRepository.existsByDeadlineIdAndThresholdDays(deadline.getId(), threshold)) {
                log.debug("DeadlineAlert: seuil J-{} déjà envoyé pour deadline {}", threshold, deadline.getId());
                continue;
            }
            notifyMembers(deadline, (int) daysUntilDue);
            recordSend(deadline, threshold);
        }
    }

    private void notifyMembers(CaseDeadline deadline, int daysRemaining) {
        UUID workspaceId = deadline.getCaseFile().getWorkspace().getId();
        String caseFileTitle = deadline.getCaseFile().getTitle();
        UUID caseFileId = deadline.getCaseFile().getId();
        String dueDateStr = deadline.getDueDate().toString();

        List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspace_Id(workspaceId);
        for (WorkspaceMember member : members) {
            String email = member.getUser().getEmail();
            if (email == null || email.isBlank()) {
                log.warn("DeadlineAlert: member {} has no email — skipping", member.getUser().getId());
                continue;
            }
            try {
                emailService.sendDeadlineAlert(email, caseFileTitle, caseFileId,
                        deadline.getLabel(), dueDateStr, daysRemaining);
            } catch (Exception e) {
                log.warn("DeadlineAlert: failed to send to {} — {}", email, e.getMessage());
            }
            try {
                inAppNotificationService.create(
                        member.getUser().getId(), workspaceId,
                        NotificationType.DEADLINE_APPROACHING,
                        "Délai J-" + daysRemaining + " : " + deadline.getLabel(),
                        caseFileTitle + " — Échéance le " + dueDateStr,
                        "/case-files/" + caseFileId);
            } catch (Exception e) {
                log.warn("DeadlineAlert: failed to create in-app notification for member {} — {}", member.getUser().getId(), e.getMessage());
            }
        }
    }

    private void recordSend(CaseDeadline deadline, int thresholdDays) {
        DeadlineAlertSend send = new DeadlineAlertSend();
        send.setDeadline(deadline);
        send.setThresholdDays(thresholdDays);
        alertSendRepository.save(send);
    }

    private void processDeadline(CaseDeadline deadline, LocalDate today) {
        int daysRemaining = (int) (deadline.getDueDate().toEpochDay() - today.toEpochDay());
        notifyMembers(deadline, daysRemaining);
    }

    int[] parseThresholds(String alertThresholds) {
        if (alertThresholds == null || alertThresholds.isBlank()) return new int[0];
        return Arrays.stream(alertThresholds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .mapToInt(Integer::parseInt)
                .toArray();
    }
}
