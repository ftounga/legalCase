package fr.ailegalcase.blog.scheduler;

import fr.ailegalcase.blog.cost.IaCostTracker;
import fr.ailegalcase.blog.cost.IaProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

/**
 * F-120 garde-fou n° 6 — alerte email quotidienne quand la projection mensuelle
 * dépasse un seuil. Best-effort : si JavaMailSender absent ou crash, log WARN.
 */
@Component
public class BlogCostAlertJob {

    private static final Logger log = LoggerFactory.getLogger(BlogCostAlertJob.class);

    private final IaCostTracker costTracker;
    private final JavaMailSender mailSender;
    private final BigDecimal anthropicAlertEur;
    private final BigDecimal openaiAlertEur;
    private final String superAdminEmail;
    private final String mailFrom;

    public BlogCostAlertJob(IaCostTracker costTracker,
                            JavaMailSender mailSender,
                            @Value("${blog.cost-alert.anthropic-eur:15}") BigDecimal anthropicAlertEur,
                            @Value("${blog.cost-alert.openai-eur:10}") BigDecimal openaiAlertEur,
                            @Value("${blog.cost-alert.super-admin-email:}") String superAdminEmail,
                            @Value("${spring.mail.from:no-reply@ng-itconsulting.com}") String mailFrom) {
        this.costTracker = costTracker;
        this.mailSender = mailSender;
        this.anthropicAlertEur = anthropicAlertEur;
        this.openaiAlertEur = openaiAlertEur;
        this.superAdminEmail = superAdminEmail;
        this.mailFrom = mailFrom;
    }

    @Scheduled(cron = "${blog.cost-alert.cron:0 0 9 * * *}", zone = "UTC")
    public void checkProjectedCost() {
        BigDecimal anthropicProjection = projectMonthlyCost(IaProvider.ANTHROPIC);
        BigDecimal openaiProjection = projectMonthlyCost(IaProvider.OPENAI);

        boolean anthropicOver = anthropicProjection.compareTo(anthropicAlertEur) > 0;
        boolean openaiOver = openaiProjection.compareTo(openaiAlertEur) > 0;

        if (!anthropicOver && !openaiOver) {
            log.debug("Blog cost projection within thresholds: ANTHROPIC={}€, OPENAI={}€",
                    anthropicProjection, openaiProjection);
            return;
        }
        sendAlert(anthropicProjection, openaiProjection, anthropicOver, openaiOver);
    }

    BigDecimal projectMonthlyCost(IaProvider provider) {
        BigDecimal spent = costTracker.monthlySpent(provider);
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        int dayOfMonth = today.getDayOfMonth();
        int lengthOfMonth = YearMonth.from(today).lengthOfMonth();
        if (dayOfMonth == 0) {
            return spent;
        }
        return spent
                .multiply(BigDecimal.valueOf(lengthOfMonth))
                .divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);
    }

    private void sendAlert(BigDecimal anthropicProjection,
                           BigDecimal openaiProjection,
                           boolean anthropicOver,
                           boolean openaiOver) {
        if (superAdminEmail == null || superAdminEmail.isBlank()) {
            log.warn("Blog cost alert triggered but no super-admin email configured: " +
                    "ANTHROPIC={}€ (over={}) / OPENAI={}€ (over={})",
                    anthropicProjection, anthropicOver, openaiProjection, openaiOver);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(mailFrom);
            msg.setTo(superAdminEmail);
            msg.setSubject("[LegalCase Blog] Alerte coût IA mensuel projeté");
            msg.setText(("Projection coûts blog F-120 (fin de mois, linéaire) :%n" +
                    "  Anthropic : %s €  (seuil : %s €)%n" +
                    "  OpenAI    : %s €  (seuil : %s €)%n%n" +
                    "Action recommandée : vérifier le scheduler et les caps.")
                    .formatted(anthropicProjection, anthropicAlertEur, openaiProjection, openaiAlertEur));
            mailSender.send(msg);
            log.info("Blog cost alert email sent to {} (ANTHROPIC={}€, OPENAI={}€)",
                    superAdminEmail, anthropicProjection, openaiProjection);
        } catch (Exception e) {
            log.warn("Failed to send blog cost alert email: {}", e.getMessage());
        }
    }
}
