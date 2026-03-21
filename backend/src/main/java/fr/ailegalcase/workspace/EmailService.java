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
    private final String mailFrom;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.enabled:false}") boolean mailEnabled,
                        @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
                        @Value("${app.mail.from:${spring.mail.username:}}") String mailFrom) {
        this.mailSender = mailSender;
        this.mailEnabled = mailEnabled;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
    }

    public void sendEmailVerification(String toEmail, String token) {
        if (!mailEnabled) {
            log.debug("Mail disabled — email verification skipped for {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Validez votre adresse email — AI LegalCase");
            message.setText(
                    "Bonjour,\n\n" +
                    "Merci de vous être inscrit(e) sur AI LegalCase.\n\n" +
                    "Cliquez sur le lien ci-dessous pour valider votre adresse email :\n" +
                    frontendUrl + "/verify-email?token=" + token + "\n\n" +
                    "Ce lien expire dans 24 heures.\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Email verification sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send email verification to {} — {}", toEmail, e.getMessage());
            throw e;
        }
    }

    public void sendPasswordReset(String toEmail, String token) {
        if (!mailEnabled) {
            log.debug("Mail disabled — password reset email skipped for {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Réinitialisation de votre mot de passe — AI LegalCase");
            message.setText(
                    "Bonjour,\n\n" +
                    "Vous avez demandé la réinitialisation de votre mot de passe sur AI LegalCase.\n\n" +
                    "Cliquez sur le lien ci-dessous pour choisir un nouveau mot de passe :\n" +
                    frontendUrl + "/reset-password?token=" + token + "\n\n" +
                    "Ce lien expire dans 24 heures.\n\n" +
                    "Si vous n'êtes pas à l'origine de cette demande, ignorez cet email.\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException e) {
            log.error("Failed to send password reset email to {} — {}", toEmail, e.getMessage());
            throw e;
        }
    }

    public void sendInvitation(String toEmail, String workspaceName, String token) {
        if (!mailEnabled) {
            log.debug("Mail disabled — invitation email skipped for {}", toEmail);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
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
