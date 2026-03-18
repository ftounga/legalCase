package fr.ailegalcase.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

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
        EmailService service = new EmailService(mailSender, true, "http://localhost:4200");

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
        EmailService service = new EmailService(mailSender, false, "http://localhost:4200");

        service.sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    // U-03 : SMTP échoue → pas d'exception propagée (fail-open)
    @Test
    void sendInvitation_smtpFailure_doesNotThrow() {
        EmailService service = new EmailService(mailSender, true, "http://localhost:4200");
        doThrow(new MailSendException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatNoException().isThrownBy(
                () -> service.sendInvitation("invitee@example.com", "Mon Cabinet", "tok-123"));
    }
}
