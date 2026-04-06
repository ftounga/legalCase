package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.notification.InAppNotificationService;
import fr.ailegalcase.notification.NotificationType;
import fr.ailegalcase.workspace.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RequalificationAlertService {

    private static final Logger log = LoggerFactory.getLogger(RequalificationAlertService.class);

    private final EmailService emailService;
    private final CaseFileRepository caseFileRepository;
    private final InAppNotificationService inAppNotificationService;

    public RequalificationAlertService(EmailService emailService,
                                        CaseFileRepository caseFileRepository,
                                        InAppNotificationService inAppNotificationService) {
        this.emailService = emailService;
        this.caseFileRepository = caseFileRepository;
        this.inAppNotificationService = inAppNotificationService;
    }

    @EventListener
    public void onRequalification(ProcedureCheckRequalifiedEvent event) {
        try {
            emailService.sendRequalificationAlert(
                    event.creatorEmail(),
                    event.caseFileId(),
                    event.caseFileTitle(),
                    event.requalifiedChecks()
            );
        } catch (Exception e) {
            log.warn("RequalificationAlert: failed to send email for caseFile {} — {}", event.caseFileId(), e.getMessage());
        }

        try {
            UUID userId = caseFileRepository.findCreatedByUserIdById(event.caseFileId()).orElse(null);
            UUID workspaceId = caseFileRepository.findWorkspaceIdById(event.caseFileId()).orElse(null);
            if (userId != null && workspaceId != null) {
                String checks = event.requalifiedChecks().stream()
                        .map(ProcedureCheckRequalifiedEvent.RequalifiedCheck::description)
                        .limit(3)
                        .collect(Collectors.joining(", "));
                inAppNotificationService.create(userId, workspaceId,
                        NotificationType.PROCEDURE_REQUALIFIED,
                        "Procédures requalifiées : " + event.caseFileTitle(),
                        checks + (event.requalifiedChecks().size() > 3 ? "..." : ""),
                        "/case-files/" + event.caseFileId() + "/synthesis");
            }
        } catch (Exception e) {
            log.warn("RequalificationAlert: failed to create in-app notification for caseFile {} — {}", event.caseFileId(), e.getMessage());
        }
    }
}
