package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
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

    public AnalysisNotificationService(CaseFileRepository caseFileRepository, EmailService emailService) {
        this.caseFileRepository = caseFileRepository;
        this.emailService = emailService;
    }

    @EventListener
    public void onAnalysisStatusEvent(AnalysisStatusEvent event) {
        if (event.status() != AnalysisStatus.DONE) return;
        if (event.jobType() != JobType.CASE_ANALYSIS && event.jobType() != JobType.ENRICHED_ANALYSIS) return;

        UUID caseFileId = event.caseFileId();

        String title = caseFileRepository.findTitleById(caseFileId).orElse(null);
        if (title == null) {
            log.warn("AnalysisNotification: CaseFile {} not found — email skipped", caseFileId);
            return;
        }

        String email = caseFileRepository.findCreatorEmailById(caseFileId).orElse(null);
        if (email == null) {
            log.warn("AnalysisNotification: creator email not found for CaseFile {} — email skipped", caseFileId);
            return;
        }

        try {
            emailService.sendAnalysisDone(email, caseFileId, title, event.jobType());
        } catch (Exception e) {
            log.warn("AnalysisNotification: failed to send email for CaseFile {} — {}", caseFileId, e.getMessage());
        }
    }
}
