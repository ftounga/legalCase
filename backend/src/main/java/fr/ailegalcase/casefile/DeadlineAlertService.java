package fr.ailegalcase.casefile;

import fr.ailegalcase.workspace.EmailService;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class DeadlineAlertService {

    private static final Logger log = LoggerFactory.getLogger(DeadlineAlertService.class);
    private static final int[] ALERT_DAYS = {15, 7};

    private final CaseDeadlineRepository deadlineRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final EmailService emailService;

    public DeadlineAlertService(CaseDeadlineRepository deadlineRepository,
                                WorkspaceMemberRepository workspaceMemberRepository,
                                EmailService emailService) {
        this.deadlineRepository = deadlineRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendDailyAlerts() {
        LocalDate today = LocalDate.now();
        List<LocalDate> targetDates = List.of(today.plusDays(15), today.plusDays(7));

        List<CaseDeadline> deadlines = deadlineRepository
                .findByDueDateInAndCaseFileDeletedAtIsNull(targetDates);

        if (deadlines.isEmpty()) {
            log.debug("DeadlineAlert: no deadlines at J-15/J-7 today ({})", today);
            return;
        }

        log.info("DeadlineAlert: {} deadline(s) to notify for {}", deadlines.size(), today);

        for (CaseDeadline deadline : deadlines) {
            try {
                processDeadline(deadline, today);
            } catch (Exception e) {
                log.error("DeadlineAlert: unexpected error for deadline {} — {}", deadline.getId(), e.getMessage());
            }
        }
    }

    private void processDeadline(CaseDeadline deadline, LocalDate today) {
        int daysRemaining = (int) (deadline.getDueDate().toEpochDay() - today.toEpochDay());
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
        }
    }
}
