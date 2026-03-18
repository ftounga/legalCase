package fr.ailegalcase.workspace;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:false}") boolean mailEnabled,
                        @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.frontendUrl = frontendUrl;
    }

    public void sendInvitation(String toEmail, String workspaceName, String token) {
        if (!mailEnabled) {
            log.debug("Mail disabled — invitation email skipped for {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Invitation à rejoindre " + workspaceName + " sur AI LegalCase");
            message.setText(
                    "Bonjour,\n\n" +
                    "Vous avez été invité(e) à rejoindre le workspace \"" + workspaceName + "\" sur AI LegalCase.\n\n" +
                    "Cliquez sur le lien ci-dessous pour accepter l'invitation :\n" +
                    frontendUrl + "/invite?token=" + token + "\n\n" +
                    "Ce lien expire dans 7 jours.\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Invitation email sent to {}", toEmail);
        } catch (MailException e) {
            log.warn("Failed to send invitation email to {} — {}", toEmail, e.getMessage());
        }
    }
}
