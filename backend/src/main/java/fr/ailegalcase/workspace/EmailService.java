package fr.ailegalcase.workspace;

import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.analysis.ProcedureCheckRequalifiedEvent;
import fr.ailegalcase.auth.User;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailSendRepository emailSendRepository;
    private final boolean mailEnabled;
    private final String frontendUrl;
    private final String mailFrom;
    private final String contactTeamEmail;

    public EmailService(JavaMailSender mailSender,
                        EmailSendRepository emailSendRepository,
                        @Value("${app.mail.enabled:false}") boolean mailEnabled,
                        @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
                        @Value("${app.mail.from:${spring.mail.username:}}") String mailFrom,
                        @Value("${app.contact.team-email:ai-legalcase@ng-itconsulting.com}") String contactTeamEmail) {
        this.mailSender = mailSender;
        this.emailSendRepository = emailSendRepository;
        this.mailEnabled = mailEnabled;
        this.frontendUrl = frontendUrl;
        this.mailFrom = mailFrom;
        this.contactTeamEmail = contactTeamEmail;
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

    public void sendAnalysisDone(String toEmail, UUID caseFileId, String caseFileTitle, JobType jobType) {
        if (!mailEnabled) {
            log.debug("Mail disabled — analysis-done email skipped for {}", toEmail);
            return;
        }

        String typeLabel = jobType == JobType.ENRICHED_ANALYSIS ? "enrichie" : "standard";
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Analyse terminée — " + caseFileTitle + " — AI LegalCase");
            message.setText(
                    "Bonjour,\n\n" +
                    "L'analyse " + typeLabel + " de votre dossier \"" + caseFileTitle + "\" est terminée.\n\n" +
                    "Accédez à votre dossier ici :\n" +
                    frontendUrl + "/case-files/" + caseFileId + "\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Analysis-done email sent to {} for caseFile {}", toEmail, caseFileId);
        } catch (MailException e) {
            log.warn("Failed to send analysis-done email to {} for caseFile {} — {}", toEmail, caseFileId, e.getMessage());
        }
    }

    public void sendRequalificationAlert(String toEmail, UUID caseFileId, String caseFileTitle,
                                          List<ProcedureCheckRequalifiedEvent.RequalifiedCheck> checks) {
        if (!mailEnabled) {
            log.debug("Mail disabled — requalification alert skipped for {}", toEmail);
            return;
        }
        try {
            StringBuilder body = new StringBuilder();
            body.append("Bonjour,\n\n")
                .append("L'IA a réévalué ")
                .append(checks.size() == 1 ? "un point procédural" : checks.size() + " points procéduraux")
                .append(" sur le dossier \"").append(caseFileTitle).append("\" :\n\n");
            for (ProcedureCheckRequalifiedEvent.RequalifiedCheck check : checks) {
                String statusLabel = check.newStatus().name().equals("NON_COMPLIANT") ? "Non conforme" : "À vérifier";
                body.append("• ").append(check.description()).append(" → ").append(statusLabel).append("\n");
                if (check.raison() != null && !check.raison().isBlank()) {
                    body.append("  Raison : ").append(check.raison()).append("\n");
                }
                body.append("\n");
            }
            body.append("Accédez à votre dossier ici :\n")
                .append(frontendUrl).append("/case-files/").append(caseFileId).append("\n\n")
                .append("L'équipe AI LegalCase");

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Point(s) procédural(aux) réévalué(s) sur \"" + caseFileTitle + "\" — action requise");
            message.setText(body.toString());
            mailSender.send(message);
            log.info("Requalification alert sent to {} for caseFile {}", toEmail, caseFileId);
        } catch (MailException e) {
            log.warn("Failed to send requalification alert to {} for caseFile {} — {}", toEmail, caseFileId, e.getMessage());
        }
    }

    public void sendDeadlineAlert(String toEmail, String caseFileTitle, UUID caseFileId,
                                   String deadlineLabel, String dueDateStr, int daysRemaining) {
        if (!mailEnabled) {
            log.debug("Mail disabled — deadline alert skipped for {}", toEmail);
            return;
        }

        String daysText = daysRemaining == 0 ? "aujourd'hui"
                : "dans " + daysRemaining + " jour" + (daysRemaining > 1 ? "s" : "");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Délai J-" + daysRemaining + " : " + deadlineLabel + " — " + caseFileTitle);
            message.setText(
                    "Bonjour,\n\n" +
                    "Le délai \"" + deadlineLabel + "\" du dossier \"" + caseFileTitle + "\" arrive à échéance " + daysText + " (" + dueDateStr + ").\n\n" +
                    "Accédez à votre dossier ici :\n" +
                    frontendUrl + "/case-files/" + caseFileId + "\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Deadline alert sent to {} for deadline '{}' on {}", toEmail, deadlineLabel, dueDateStr);
        } catch (MailException e) {
            log.warn("Failed to send deadline alert to {} for '{}' — {}", toEmail, deadlineLabel, e.getMessage());
        }
    }

    // ── Onboarding sequence ──────────────────────────────────────────────────

    @Transactional
    public void sendOnboardingWelcome(User user) {
        sendOnboarding(user, EmailSend.EmailType.ONBOARDING_WELCOME,
                "Bienvenue sur AI LegalCase — votre workspace est prêt",
                "Bonjour " + firstName(user) + ",\n\n" +
                "Votre workspace AI LegalCase est prêt.\n\n" +
                "En quelques minutes, vous pouvez :\n" +
                "• Créer votre premier dossier\n" +
                "• Déposer vos documents (contrat, courrier, jugement)\n" +
                "• Lancer l'analyse IA et obtenir une synthèse complète\n\n" +
                "Accédez à votre espace ici :\n" +
                frontendUrl + "\n\n" +
                "L'équipe AI LegalCase"
        );
    }

    @Transactional
    public void sendOnboardingTipAnalysis(User user) {
        sendOnboarding(user, EmailSend.EmailType.ONBOARDING_TIP_ANALYSIS,
                "Analysez votre premier dossier en 3 clics — AI LegalCase",
                "Bonjour " + firstName(user) + ",\n\n" +
                "Vous n'avez pas encore lancé d'analyse ? Voici comment ça marche :\n\n" +
                "1. Créez un dossier (Nouveau dossier)\n" +
                "2. Déposez vos PDFs (contrat, courriers, jugements)\n" +
                "3. Cliquez sur « Analyser le dossier »\n\n" +
                "L'IA lit l'intégralité de vos documents et génère une synthèse structurée : " +
                "faits clés, risques juridiques, timeline, points de droit.\n\n" +
                "Essayez maintenant :\n" +
                frontendUrl + "\n\n" +
                "L'équipe AI LegalCase"
        );
    }

    @Transactional
    public void sendOnboardingTipShare(User user) {
        sendOnboarding(user, EmailSend.EmailType.ONBOARDING_TIP_SHARE,
                "Partagez une synthèse avec votre client — AI LegalCase",
                "Bonjour " + firstName(user) + ",\n\n" +
                "Saviez-vous que vous pouvez partager la synthèse d'un dossier avec votre client en un clic ?\n\n" +
                "Sur la page d'un dossier analysé, cliquez sur « Partager ».\n" +
                "Un lien sécurisé est généré — votre client peut consulter la synthèse sans créer de compte.\n\n" +
                "Accédez à vos dossiers :\n" +
                frontendUrl + "\n\n" +
                "L'équipe AI LegalCase"
        );
    }

    @Transactional
    public void sendOnboardingBeforeExpiry(User user) {
        sendOnboarding(user, EmailSend.EmailType.ONBOARDING_BEFORE_EXPIRY,
                "Votre essai AI LegalCase se termine dans 3 jours",
                "Bonjour " + firstName(user) + ",\n\n" +
                "Votre période d'essai gratuite se termine dans 3 jours.\n\n" +
                "Pour continuer à utiliser AI LegalCase sans interruption, passez au plan Solo ou Pro.\n\n" +
                "Voir les tarifs :\n" +
                frontendUrl + "/billing\n\n" +
                "Des questions ? Répondez directement à cet email.\n\n" +
                "L'équipe AI LegalCase"
        );
    }

    @Transactional
    public void sendOnboardingExpired(User user) {
        sendOnboarding(user, EmailSend.EmailType.ONBOARDING_EXPIRED,
                "Votre essai AI LegalCase est terminé — reprenez votre activité",
                "Bonjour " + firstName(user) + ",\n\n" +
                "Votre essai gratuit est maintenant terminé.\n\n" +
                "Vos dossiers et analyses sont conservés. Pour y accéder à nouveau, choisissez un plan.\n\n" +
                "Reprendre maintenant :\n" +
                frontendUrl + "/billing\n\n" +
                "L'équipe AI LegalCase"
        );
    }

    private void sendOnboarding(User user, EmailSend.EmailType type, String subject, String body) {
        if (!mailEnabled) {
            log.debug("Mail disabled — {} skipped for {}", type, user.getEmail());
            return;
        }
        if (emailSendRepository.existsByUserAndEmailType(user, type)) {
            log.info("Onboarding email {} already sent for user {} — skipping", type, user.getId());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            EmailSend record = new EmailSend();
            record.setUser(user);
            record.setEmailType(type);
            record.setSentAt(Instant.now());
            emailSendRepository.save(record);

            log.info("Onboarding email {} sent to {}", type, user.getEmail());
        } catch (MailException e) {
            log.warn("Failed to send onboarding email {} to {} — {}", type, user.getEmail(), e.getMessage());
        }
    }

    // ── Contact form ─────────────────────────────────────────────────────────

    public void sendContactToTeam(fr.ailegalcase.contact.ContactRequest req) {
        if (!mailEnabled) {
            log.debug("Mail disabled — contact-to-team skipped for {}", req.email());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(contactTeamEmail);
            message.setReplyTo(req.email());
            message.setSubject("[Contact] " + req.sujet());
            message.setText(buildTeamBody(req));
            mailSender.send(message);
            log.info("Contact email sent to team from {}", req.email());
        } catch (MailException e) {
            log.warn("Failed to send contact email to team from {} — {}", req.email(), e.getMessage());
        }
    }

    public void sendContactConfirmation(fr.ailegalcase.contact.ContactRequest req) {
        if (!mailEnabled) {
            log.debug("Mail disabled — contact confirmation skipped for {}", req.email());
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(req.email());
            message.setSubject("Nous avons bien reçu votre message — AI LegalCase");
            message.setText(
                    "Bonjour " + req.nom() + ",\n\n" +
                    "Nous avons bien reçu votre message concernant : \"" + req.sujet() + "\".\n\n" +
                    "Notre équipe vous répondra dans les plus brefs délais à cette adresse.\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Contact confirmation sent to {}", req.email());
        } catch (MailException e) {
            log.warn("Failed to send contact confirmation to {} — {}", req.email(), e.getMessage());
        }
    }

    private String buildTeamBody(fr.ailegalcase.contact.ContactRequest req) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nouveau message de contact\n\n");
        sb.append("Nom       : ").append(req.nom()).append("\n");
        sb.append("Email     : ").append(req.email()).append("\n");
        if (req.telephone() != null && !req.telephone().isBlank()) {
            sb.append("Téléphone : ").append(req.telephone()).append("\n");
        }
        sb.append("Sujet     : ").append(req.sujet()).append("\n\n");
        sb.append("Message :\n").append(req.message());
        return sb.toString();
    }

    private String firstName(User user) {
        return (user.getFirstName() != null && !user.getFirstName().isBlank())
                ? user.getFirstName() : "Maître";
    }

    // ── Monthly newsletter ────────────────────────────────────────────────────

    @Transactional
    public void sendMonthlyNewsletter(User user, long analysisCount, long activeCaseFiles,
                                       long documentsUploaded, String featureTitle, String featureDescription,
                                       String monthLabel) {
        if (!mailEnabled) {
            log.debug("Mail disabled — newsletter skipped for {}", user.getEmail());
            return;
        }
        String subject = "[AI LegalCase] Votre récapitulatif de " + monthLabel;
        String body =
                "Bonjour " + firstName(user) + ",\n\n" +
                "Voici votre récapitulatif pour le mois de " + monthLabel + " :\n\n" +
                "Votre activité\n" +
                "- " + analysisCount + " analyse(s) lancée(s)\n" +
                "- " + activeCaseFiles + " dossier(s) actif(s)\n" +
                "- " + documentsUploaded + " document(s) uploadé(s)\n\n" +
                "Feature du mois : " + featureTitle + "\n" +
                featureDescription + "\n\n" +
                "Accédez à votre espace :\n" +
                frontendUrl + "\n\n" +
                "L'équipe AI LegalCase";

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(user.getEmail());
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);

            EmailSend record = new EmailSend();
            record.setUser(user);
            record.setEmailType(EmailSend.EmailType.NEWSLETTER_MONTHLY);
            record.setSentAt(Instant.now());
            emailSendRepository.save(record);

            log.info("Monthly newsletter sent to {}", user.getEmail());
        } catch (MailException e) {
            log.warn("Failed to send monthly newsletter to {} — {}", user.getEmail(), e.getMessage());
        }
    }

    public void sendReferentialAlert(String toEmail, String frontendUrl) {
        if (!mailEnabled) {
            log.debug("Mail disabled — referential alert skipped for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Mise à jour requise — Guides & barèmes — AI LegalCase");
            message.setText(
                    "Bonjour,\n\n" +
                    "L'IA a détecté que certaines valeurs de vos référentiels métier (barèmes, délais légaux) " +
                    "pourraient ne plus être à jour par rapport aux textes officiels en vigueur.\n\n" +
                    "Veuillez consulter l'écran Guides & barèmes pour valider ou appliquer les valeurs suggérées :\n" +
                    frontendUrl + "/referentials\n\n" +
                    "Aucune modification n'a été appliquée automatiquement — votre confirmation est requise.\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Referential alert email sent to {}", toEmail);
        } catch (MailException e) {
            log.warn("Failed to send referential alert to {} — {}", toEmail, e.getMessage());
        }
    }

    public void sendReferentialAlertReminder(String toEmail, String frontendUrl) {
        if (!mailEnabled) {
            log.debug("Mail disabled — referential alert reminder skipped for {}", toEmail);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(mailFrom);
            message.setTo(toEmail);
            message.setSubject("Rappel — Mise à jour en attente — Guides & barèmes — AI LegalCase");
            message.setText(
                    "Bonjour,\n\n" +
                    "Des mises à jour de vos référentiels métier (barèmes, délais légaux) sont toujours en attente " +
                    "de votre validation.\n\n" +
                    "Veuillez consulter l'écran Guides & barèmes :\n" +
                    frontendUrl + "/referentials\n\n" +
                    "L'équipe AI LegalCase"
            );
            mailSender.send(message);
            log.info("Referential alert reminder sent to {}", toEmail);
        } catch (MailException e) {
            log.warn("Failed to send referential alert reminder to {} — {}", toEmail, e.getMessage());
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
