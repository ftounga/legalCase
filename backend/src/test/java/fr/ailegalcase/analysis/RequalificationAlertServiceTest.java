package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.notification.InAppNotificationService;
import fr.ailegalcase.workspace.EmailService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class RequalificationAlertServiceTest {

    private final EmailService emailService = mock(EmailService.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final InAppNotificationService inAppNotificationService = mock(InAppNotificationService.class);
    private final RequalificationAlertService service = new RequalificationAlertService(
            emailService, caseFileRepository, inAppNotificationService);

    @Test
    void onRequalification_callsSendRequalificationAlert() {
        UUID caseFileId = UUID.randomUUID();
        List<ProcedureCheckRequalifiedEvent.RequalifiedCheck> checks = List.of(
                new ProcedureCheckRequalifiedEvent.RequalifiedCheck(
                        "Entretien préalable tenu", ProcedureCheckStatus.NON_COMPLIANT, "Contredit par pièce 3")
        );
        ProcedureCheckRequalifiedEvent event = new ProcedureCheckRequalifiedEvent(
                caseFileId, "Affaire Dupont", "avocat@cabinet.fr", checks);

        service.onRequalification(event);

        verify(emailService).sendRequalificationAlert("avocat@cabinet.fr", caseFileId, "Affaire Dupont", checks);
    }

    @Test
    void onRequalification_emailServiceThrows_doesNotPropagateException() {
        UUID caseFileId = UUID.randomUUID();
        ProcedureCheckRequalifiedEvent event = new ProcedureCheckRequalifiedEvent(
                caseFileId, "Affaire Dupont", "avocat@cabinet.fr", List.of());

        doThrow(new RuntimeException("SMTP error")).when(emailService)
                .sendRequalificationAlert(any(), any(), any(), any());

        // fail-open : aucune exception ne doit se propager
        service.onRequalification(event);
    }
}
