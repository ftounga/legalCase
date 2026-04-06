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

@Service
public class AnalysisNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisNotificationService.class);

    private final CaseFileRepository caseFileRepository;
    private final EmailService emailService;
    private final InAppNotificationService inAppNotificationService;

    public AnalysisNotificationService(CaseFileRepository caseFileRepository,
                                        EmailService emailService,
                                        InAppNotificationService inAppNotificationService) {
        this.caseFileRepository = caseFileRepository;
        this.emailService = emailService;
        this.inAppNotificationService = inAppNotificationService;
    }

    @EventListener
    public void onAnalysisStatusEvent(AnalysisStatusEvent event) {
        if (event.jobType() != JobType.CASE_ANALYSIS && event.jobType() != JobType.ENRICHED_ANALYSIS) return;

        UUID caseFileId = event.caseFileId();

        if (event.status() == AnalysisStatus.FAILED) {
            createInAppNotification(caseFileId, NotificationType.ANALYSIS_FAILED,
                    "Analyse échouée", "L'analyse de votre dossier a échoué.");
            return;
        }

        if (event.status() != AnalysisStatus.DONE) return;

        String title = caseFileRepository.findTitleById(caseFileId).orElse(null);
        if (title == null) {
            log.warn("AnalysisNotification: CaseFile {} not found — skipped", caseFileId);
            return;
        }

        String email = caseFileRepository.findCreatorEmailById(caseFileId).orElse(null);
        if (email == null) {
            log.warn("AnalysisNotification: creator email not found for CaseFile {} — email skipped", caseFileId);
        } else {
            try {
                emailService.sendAnalysisDone(email, caseFileId, title, event.jobType());
            } catch (Exception e) {
                log.warn("AnalysisNotification: failed to send email for CaseFile {} — {}", caseFileId, e.getMessage());
            }
        }

        createInAppNotification(caseFileId, NotificationType.ANALYSIS_DONE,
                "Analyse terminée : " + title, event.jobType().name() + " — " + title);
    }

    private void createInAppNotification(UUID caseFileId, NotificationType type,
                                          String title, String message) {
        try {
            UUID userId = caseFileRepository.findCreatedByUserIdById(caseFileId).orElse(null);
            UUID workspaceId = caseFileRepository.findWorkspaceIdById(caseFileId).orElse(null);
            if (userId == null || workspaceId == null) {
                log.warn("AnalysisNotification: cannot resolve user/workspace for CaseFile {} — in-app skipped", caseFileId);
                return;
            }
            inAppNotificationService.create(userId, workspaceId, type, title, message,
                    "/case-files/" + caseFileId + "/synthesis");
        } catch (Exception e) {
            log.warn("AnalysisNotification: failed to create in-app notification for CaseFile {} — {}", caseFileId, e.getMessage());
        }
    }
}
