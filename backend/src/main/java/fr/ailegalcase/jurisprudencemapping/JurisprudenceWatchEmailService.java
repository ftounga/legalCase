package fr.ailegalcase.jurisprudencemapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * F-JU-01 / SF-JU-01-02 — envoi du récap mensuel ou de l'alerte
 * « intervention requise » au fondateur.
 *
 * <p>Email texte simple via {@link JavaMailSender}. Pas de HTML / template en
 * V1 — V2 si signal terrain.</p>
 */
@Service
public class JurisprudenceWatchEmailService {

    private static final Logger log = LoggerFactory.getLogger(JurisprudenceWatchEmailService.class);
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String founderEmail;
    private final String appBaseUrl;

    public JurisprudenceWatchEmailService(JavaMailSender mailSender,
                                          @Value("${spring.mail.username:noreply@legalcase.fr}") String fromEmail,
                                          @Value("${jurisprudence.watch.recap-recipient:tounga.franck@ng-itconsulting.com}") String founderEmail,
                                          @Value("${app.base-url:https://legalcase.fr}") String appBaseUrl) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.founderEmail = founderEmail;
        this.appBaseUrl = appBaseUrl;
    }

    public void sendMonthlyRecap(JurisprudenceWatchRunSummary summary) {
        String mois = summary.periodStartInclusive().format(MONTH_FORMAT);
        String subject = "[LegalCase] Veille jurisprudentielle — " + mois;
        String body = """
                Récap veille jurisprudentielle %s

                Arrêts récupérés : %d
                Mappings évalués : %d

                Auto-actions :
                  - Confirmations : %d
                  - Ajouts compléments : %d
                  - Remplacements : %d
                  - Archivages : %d

                Flags PENDING (à arbitrer) : %d
                Mappings skipped (Claude indisponible) : %d

                %s
                Tableau de bord : %s/super-admin/jurisprudence-watch
                """.formatted(
                mois,
                summary.arretsRecuperes(),
                summary.mappingsEvalues(),
                summary.autoConfirm(),
                summary.autoAdd(),
                summary.autoReplace(),
                summary.autoArchive(),
                summary.flagsPending(),
                summary.skipped(),
                summary.aborted() ? "⚠ RUN INTERROMPU : " + summary.abortReason() + "\n" : "",
                appBaseUrl);
        send(subject, body);
    }

    public void sendAbortAlert(JurisprudenceWatchRunSummary summary) {
        String mois = summary.periodStartInclusive().format(MONTH_FORMAT);
        String subject = "[LegalCase] ALERTE veille jurisprudentielle — " + mois + " — intervention requise";
        String body = """
                Le run veille jurisprudentielle %s a été INTERROMPU.

                Raison : %s

                Actions appliquées avant interruption :
                  - Confirmations : %d
                  - Ajouts : %d
                  - Remplacements : %d
                  - Archivages : %d

                Mappings évalués : %d / %d total

                Vérifier le dashboard : %s/super-admin/jurisprudence-watch
                L'audit log conserve la trace de chaque action appliquée.
                """.formatted(
                mois,
                summary.abortReason(),
                summary.autoConfirm(),
                summary.autoAdd(),
                summary.autoReplace(),
                summary.autoArchive(),
                summary.mappingsEvalues(),
                summary.mappingsEvalues() + summary.skipped(),
                appBaseUrl);
        send(subject, body);
    }

    void send(String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(founderEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.warn("F-JU-01 — JurisprudenceWatchEmailService send failed: {}", e.getMessage());
        }
    }
}
