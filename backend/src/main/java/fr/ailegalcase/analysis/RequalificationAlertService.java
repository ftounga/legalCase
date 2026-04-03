package fr.ailegalcase.analysis;

import fr.ailegalcase.workspace.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class RequalificationAlertService {

    private static final Logger log = LoggerFactory.getLogger(RequalificationAlertService.class);

    private final EmailService emailService;

    public RequalificationAlertService(EmailService emailService) {
        this.emailService = emailService;
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
    }
}
