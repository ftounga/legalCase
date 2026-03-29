package fr.ailegalcase.workspace;

import fr.ailegalcase.analysis.JobType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;

    // U-01 : mail activé → JavaMailSender.send() appelé avec les bons paramètres
    @Test
    void sendInvitation_whenEnabled_sendsEmail() {
        EmailService service = new EmailService(mailSender, true, "http://localhost:4200", "noreply@test.com");

        service.sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("invitee@example.com");
        assertThat(msg.getSubject()).contains("Mon Cabinet");
        assertThat(msg.getText()).contains("http://localhost:4200/invite?token=tok-123");
    }

    // U-02 : mail désactivé → JavaMailSender.send() non appelé
    @Test
    void sendInvitation_whenDisabled_doesNotSend() {
        EmailService service = new EmailService(mailSender, false, "http://localhost:4200", "noreply@test.com");

        service.sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // U-03 : SMTP échoue → pas d'exception propagée (fail-open)
    @Test
    void sendInvitation_smtpFailure_doesNotThrow() {
        EmailService service = new EmailService(mailSender, true, "http://localhost:4200", "noreply@test.com");
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException().isThrownBy(
                () -> service.sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123"));
    }

    // U-04 : sendAnalysisDone activé → message envoyé avec bon sujet et lien
    @Test
    void sendAnalysisDone_whenEnabled_sendsEmail() {
        EmailService service = new EmailService(mailSender, true, "http://localhost:4200", "noreply@test.com");
        UUID caseFileId = UUID.randomUUID();

        service.sendAnalysisDone("avocat@example.com", caseFileId, "Dossier Dupont", JobType.CASE_ANALYSIS);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();
        assertThat(msg.getTo()).containsExactly("avocat@example.com");
        assertThat(msg.getSubject()).contains("Dossier Dupont");
        assertThat(msg.getText()).contains("http://localhost:4200/case-files/" + caseFileId);
        assertThat(msg.getText()).contains("standard");
    }

    // U-05 : sendAnalysisDone désactivé → aucun envoi
    @Test
    void sendAnalysisDone_whenDisabled_doesNotSend() {
        EmailService service = new EmailService(mailSender, false, "http://localhost:4200", "noreply@test.com");

        service.sendAnalysisDone("avocat@example.com", UUID.randomUUID(), "Dossier Dupont", JobType.CASE_ANALYSIS);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // U-06 : sendAnalysisDone ENRICHED_ANALYSIS → libellé "enrichie"
    @Test
    void sendAnalysisDone_enrichedType_usesEnrichedLabel() {
        EmailService service = new EmailService(mailSender, true, "http://localhost:4200", "noreply@test.com");

        service.sendAnalysisDone("avocat@example.com", UUID.randomUUID(), "Dossier Dupont", JobType.ENRICHED_ANALYSIS);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("enrichie");
    }
}
