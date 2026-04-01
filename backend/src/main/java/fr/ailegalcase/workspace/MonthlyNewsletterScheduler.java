package fr.ailegalcase.workspace;

import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.billing.SubscriptionRepository;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;


@Service
public class MonthlyNewsletterScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonthlyNewsletterScheduler.class);

    private static final List<String[]> FEATURES = List.of(
        new String[]{"Score de risque global", "Chaque dossier est maintenant noté de 0 à 100 avec un niveau de risque FAIBLE, MOYEN ou ÉLEVÉ calculé par l'IA."},
        new String[]{"Traçabilité des sources IA", "Chaque point de la synthèse indique désormais le document source exact qui a fondé l'analyse."},
        new String[]{"Stepper de progression", "Un indicateur visuel vous guide pas à pas dans le traitement de chaque dossier."},
        new String[]{"Checklist de conformité procédurale", "Vérifiez automatiquement que toutes les étapes procédurales sont respectées."},
        new String[]{"Export PDF personnalisé", "Générez un rapport PDF professionnel de la synthèse en un clic."},
        new String[]{"Export DOCX", "Exportez la synthèse au format Word pour la modifier et l'intégrer à vos documents."},
        new String[]{"Partage sécurisé avec votre client", "Partagez la synthèse via un lien temporaire et révocable, sans compte nécessaire."},
        new String[]{"Chat avec l'IA sur votre dossier", "Posez des questions libres à l'IA directement sur le contenu de vos documents."},
        new String[]{"Analyse enrichie multi-sources", "L'IA croise tous vos documents pour produire une synthèse approfondie avec sources croisées."},
        new String[]{"Délais légaux automatiques", "Les délais importants de votre dossier sont détectés et affichés avec leur date limite."},
        new String[]{"Questions interactives de l'IA", "L'IA peut vous poser des questions ciblées pour affiner son analyse quand des informations manquent."},
        new String[]{"Tableau de bord super-admin", "Suivez les métriques de conversion et d'usage de toute la plateforme en temps réel."},
        new String[]{"Invitations membres", "Invitez des collaborateurs dans votre workspace avec gestion des rôles."},
        new String[]{"Versioning des analyses", "Chaque re-analyse crée une nouvelle version — retrouvez l'historique complet."},
        new String[]{"Analyse standard et enrichie", "Deux niveaux d'analyse : rapide pour les dossiers simples, enrichie pour les cas complexes."},
        new String[]{"Page de partage client", "Votre client accède à la synthèse via une page dédiée, sans créer de compte."},
        new String[]{"Alertes délais par email", "Recevez un email automatique J-7, J-3 et J-0 avant chaque délai critique."},
        new String[]{"Multi-domaines juridiques", "Droit du travail, droit de la famille, droit de l'immigration — un seul outil."},
        new String[]{"Prise de rendez-vous intégrée", "Planifiez une démo ou un échange directement depuis la page d'accueil."},
        new String[]{"Gestion de l'abonnement", "Passez du plan Solo au plan Pro en quelques clics, sans interruption de service."},
        new String[]{"Séquence d'onboarding guidée", "Une série d'emails vous accompagne pour tirer le meilleur parti de l'outil dès le départ."},
        new String[]{"Conformité RGPD", "Bannière de consentement, politique de confidentialité et CGU intégrées."},
        new String[]{"Workspace dédié par cabinet", "Isolation totale des données entre cabinets — vos dossiers restent les vôtres."},
        new String[]{"Authentification Google & Microsoft", "Connectez-vous avec votre compte professionnel, sans mot de passe supplémentaire."}
    );

    private static final List<String> PAID_PLANS = List.of("SOLO", "TEAM", "PRO");

    private final SubscriptionRepository subscriptionRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final EmailSendRepository emailSendRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final DocumentRepository documentRepository;
    private final EmailService emailService;

    public MonthlyNewsletterScheduler(SubscriptionRepository subscriptionRepository,
                                       WorkspaceMemberRepository workspaceMemberRepository,
                                       EmailSendRepository emailSendRepository,
                                       CaseAnalysisRepository caseAnalysisRepository,
                                       CaseFileRepository caseFileRepository,
                                       DocumentRepository documentRepository,
                                       EmailService emailService) {
        this.subscriptionRepository = subscriptionRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.emailSendRepository = emailSendRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.documentRepository = documentRepository;
        this.emailService = emailService;
    }

    @Scheduled(cron = "0 0 8 1 * *")
    public void sendMonthlyNewsletters() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        String monthLabel = today.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH));
        Instant monthStart = today.withDayOfMonth(1).minusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant now = Instant.now();
        Instant deduplicationSince = today.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        String[] feature = FEATURES.get(today.getMonthValue() % FEATURES.size());
        String featureTitle = feature[0];
        String featureDescription = feature[1];

        log.info("MonthlyNewsletter: starting for month {}", monthLabel);

        subscriptionRepository.findAll().stream()
            .filter(sub -> PAID_PLANS.contains(sub.getPlanCode()))
            .forEach(sub -> {
                try {
                    sendForWorkspace(sub.getWorkspaceId(), monthLabel, monthStart, now,
                            deduplicationSince, featureTitle, featureDescription);
                } catch (Exception e) {
                    log.warn("MonthlyNewsletter: failed for workspace {} — {}", sub.getWorkspaceId(), e.getMessage());
                }
            });

        log.info("MonthlyNewsletter: completed for month {}", monthLabel);
    }

    private void sendForWorkspace(UUID workspaceId, String monthLabel, Instant monthStart, Instant now,
                                   Instant deduplicationSince, String featureTitle, String featureDescription) {
        User owner = workspaceMemberRepository.findByWorkspace_Id(workspaceId).stream()
                .filter(m -> "OWNER".equals(m.getMemberRole()))
                .map(WorkspaceMember::getUser)
                .findFirst()
                .orElse(null);

        if (owner == null) {
            log.warn("MonthlyNewsletter: no OWNER found for workspace {} — skipping", workspaceId);
            return;
        }

        if (emailSendRepository.existsByUserAndEmailTypeAndSentAtAfter(
                owner, EmailSend.EmailType.NEWSLETTER_MONTHLY, deduplicationSince)) {
            log.info("MonthlyNewsletter: already sent this month for user {} — skipping", owner.getId());
            return;
        }

        long analyses = caseAnalysisRepository.countDoneByWorkspaceIdAndCreatedAtAfter(workspaceId, monthStart);
        long activeCaseFiles = caseFileRepository.countByWorkspace_IdAndDeletedAtIsNull(workspaceId);
        long documents = documentRepository.countByWorkspaceIdAndCreatedAtAfter(workspaceId, monthStart);

        emailService.sendMonthlyNewsletter(owner, analyses, activeCaseFiles, documents,
                featureTitle, featureDescription, monthLabel);
    }
}
