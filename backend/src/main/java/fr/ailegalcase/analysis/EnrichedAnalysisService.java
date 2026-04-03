package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.chat.ChatMessage;
import fr.ailegalcase.chat.ChatMessageRepository;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.protocol.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile({"local", "prod"})
public class EnrichedAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EnrichedAnalysisService.class);

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois la synthèse globale d'un dossier juridique ainsi que les réponses de l'avocat à des questions complémentaires.
            Produis une synthèse enrichie et mise à jour en intégrant ces nouvelles informations.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "points_juridiques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "risques": [{"texte": "...", "source": "<nom exact du fichier tel qu'il apparaît dans le prompt ci-dessus>", "extrait": "..."}], "questions_ouvertes": [...], "pieces_manquantes": [...], "points_procedure": [...], "score_risque": {"niveau": "FAIBLE"|"MOYEN"|"ELEVE", "valeur": <0-100>}}
            Pour les champs "faits", "points_juridiques" et "risques", chaque élément est un objet avec "texte" (le contenu), "source" (nom exact du fichier tel qu'il apparaît dans la synthèse précédente) et "extrait" (phrase exacte tirée du document). Si la source n'est pas identifiable, utilise "source": null et "extrait": null.
            Le champ "pieces_manquantes" liste les pièces habituellement attendues dans ce type de dossier qui sont absentes des documents fournis. Si le dossier semble complet, utilise "pieces_manquantes": [].
            Le champ "points_procedure" liste les étapes procédurales légalement requises dans ce type de dossier (ex: "Entretien préalable tenu dans les délais", "Lettre de licenciement motivée"). Si la procédure semble conforme, utilise "points_procedure": [].
            Le champ "score_risque" est obligatoire : évalue le niveau de risque global du dossier. "niveau" est l'un de "FAIBLE", "MOYEN" ou "ELEVE". "valeur" est un entier entre 0 et 100 reflétant l'intensité du risque (0 = aucun risque, 100 = risque maximum).
            Contraintes de longueur : %d entrées timeline maximum, %d faits maximum, %d points_juridiques maximum, %d risques maximum, %d questions_ouvertes maximum, %d pièces manquantes maximum, %d points procédure maximum. Sois concis.
            """;

    static String buildSystemPrompt(String legalDomain, String country, AnalysisLimitsProperties.LevelLimits limits) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country),
                limits.getTimeline(), limits.getFaits(),
                limits.getPointsJuridiques(), limits.getRisques(), limits.getQuestionsOuvertes(),
                limits.getPiecesManquantes(), limits.getPointsProcedure());
    }

    record PreparedEnrichedAnalysis(UUID analysisId, String prompt, String systemPrompt, UUID caseFileId,
                                     AnalysisLimitsProperties.LevelLimits limits) {}

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService;
    private final AnalysisQaSnapshotService analysisQaSnapshotService;
    private final AnalysisLimitsProperties analysisLimitsProperties;
    private final ChatMessageRepository chatMessageRepository;
    private final ProcedureCheckService procedureCheckService;

    @Lazy @Autowired
    private EnrichedAnalysisService self;

    public EnrichedAnalysisService(CaseAnalysisRepository caseAnalysisRepository,
                                   CaseFileRepository caseFileRepository,
                                   AiQuestionRepository aiQuestionRepository,
                                   AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                   AnalysisJobRepository analysisJobRepository,
                                   AnthropicService anthropicService,
                                   UsageEventService usageEventService,
                                   ApplicationEventPublisher eventPublisher,
                                   AnalysisDocumentSnapshotService analysisDocumentSnapshotService,
                                   AnalysisQaSnapshotService analysisQaSnapshotService,
                                   AnalysisLimitsProperties analysisLimitsProperties,
                                   ChatMessageRepository chatMessageRepository,
                                   ProcedureCheckService procedureCheckService) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
        this.analysisDocumentSnapshotService = analysisDocumentSnapshotService;
        this.analysisQaSnapshotService = analysisQaSnapshotService;
        this.analysisLimitsProperties = analysisLimitsProperties;
        this.chatMessageRepository = chatMessageRepository;
        this.procedureCheckService = procedureCheckService;
    }

    @RabbitListener(queues = RabbitMQConfig.RE_ANALYSIS_QUEUE, concurrency = "3")
    public void consumeReAnalysis(ReAnalysisMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedEnrichedAnalysis prepared = self.prepareEnrichedAnalysis(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Enriched analysis START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            result = anthropicService.analyze(prepared.systemPrompt(), prepared.prompt(), 8192);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Enriched analysis DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Enriched analysis FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeEnrichedAnalysis(prepared.analysisId(), prepared.caseFileId(), result, failure, prepared.limits());
    }

    @Transactional
    public PreparedEnrichedAnalysis prepareEnrichedAnalysis(ReAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    j.setStatus(AnalysisStatus.PROCESSING);
                    return j;
                });

        CaseAnalysis previousAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);

        if (previousAnalysis == null) {
            log.warn("No DONE case analysis found for caseFile {} — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("No previous case analysis found");
            analysisJobRepository.save(job);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Case file not found");
            analysisJobRepository.save(job);
            return null;
        }

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis enrichedAnalysis = new CaseAnalysis();
        enrichedAnalysis.setCaseFile(caseFile);
        enrichedAnalysis.setVersion(nextVersion);
        enrichedAnalysis.setAnalysisType(AnalysisType.ENRICHED);
        enrichedAnalysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        enrichedAnalysis = caseAnalysisRepository.save(enrichedAnalysis);

        analysisDocumentSnapshotService.snapshot(enrichedAnalysis.getId(), caseFile);
        analysisQaSnapshotService.snapshot(enrichedAnalysis.getId(), caseFileId);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String legalDomain = ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL";
        String country     = ws != null ? ws.getCountry()     : "FRANCE";
        AnalysisLimitsProperties.LevelLimits limits = analysisLimitsProperties.forDomain(legalDomain).getDossier();
        String systemPrompt = buildSystemPrompt(legalDomain, country, limits);
        String chatSummary = buildChatSummary(caseFileId);
        List<String> nonCompliantChecks;
        try {
            nonCompliantChecks = procedureCheckService.listNonCompliant(caseFile);
        } catch (Exception e) {
            log.warn("listNonCompliant failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            nonCompliantChecks = List.of();
        }
        List<String> toCheckChecks;
        try {
            toCheckChecks = procedureCheckService.listToCheck(caseFile);
        } catch (Exception e) {
            log.warn("listToCheck failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            toCheckChecks = List.of();
        }
        String prompt = buildEnrichedPrompt(caseFileId, previousAnalysis.getAnalysisResult(), chatSummary, nonCompliantChecks, toCheckChecks);
        return new PreparedEnrichedAnalysis(enrichedAnalysis.getId(), prompt, systemPrompt, caseFileId, limits);
    }

    @Transactional
    public void finalizeEnrichedAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure,
                                          AnalysisLimitsProperties.LevelLimits limits) {
        CaseAnalysis enrichedAnalysis = caseAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            String truncated = AnalysisJsonTruncator.truncateCaseAnalysis(result.content(), limits);
            enrichedAnalysis.setAnalysisResult(truncated);
            enrichedAnalysis.setModelUsed(result.modelUsed());
            enrichedAnalysis.setPromptTokens(result.promptTokens());
            enrichedAnalysis.setCompletionTokens(result.completionTokens());
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
            CaseAnalysisResponse.populateCounts(enrichedAnalysis, truncated);
            CaseAnalysisResponse.populateRiskScore(enrichedAnalysis, truncated);
        }
        caseAnalysisRepository.save(enrichedAnalysis);

        if (failure == null) {
            procedureCheckService.createChecks(enrichedAnalysis, enrichedAnalysis.getAnalysisResult());
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setProcessedItems(1);
        job.setStatus(enrichedAnalysis.getAnalysisStatus());
        if (enrichedAnalysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Enriched analysis failed");
            reportJobFailureToSentry(caseFileId, JobType.ENRICHED_ANALYSIS, "Enriched analysis failed");
        }
        analysisJobRepository.save(job);

        AnalysisStatus finalStatus = enrichedAnalysis.getAnalysisStatus();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, finalStatus, JobType.ENRICHED_ANALYSIS));
            }
        });

        if (finalStatus == AnalysisStatus.DONE) {
            int promptTokens = enrichedAnalysis.getPromptTokens();
            int completionTokens = enrichedAnalysis.getCompletionTokens();
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.ENRICHED_ANALYSIS,
                        promptTokens, completionTokens));
        }
    }

    private void reportJobFailureToSentry(UUID caseFileId, JobType jobType, String errorMessage) {
        try {
            if (!Sentry.isEnabled()) return;
            SentryEvent event = new SentryEvent();
            event.setLevel(SentryLevel.ERROR);
            Message msg = new Message();
            msg.setMessage("IA job FAILED: %s for caseFile %s".formatted(jobType, caseFileId));
            event.setMessage(msg);
            event.setTag("caseFileId", caseFileId.toString());
            event.setTag("jobType", jobType.name());
            event.setTag("errorMessage", errorMessage);
            Sentry.captureEvent(event);
        } catch (Exception ex) {
            log.warn("Failed to report job failure to Sentry", ex);
        }
    }

    String buildChatSummary(UUID caseFileId) {
        List<ChatMessage> messages = chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId);
        if (messages.isEmpty()) return null;

        String chatText = messages.stream()
                .map(m -> "Q: %s\nR: %s".formatted(m.getQuestion(), m.getAnswer() != null ? m.getAnswer() : "(sans réponse)"))
                .collect(Collectors.joining("\n\n"));

        String systemPrompt = """
                Tu es un assistant juridique. Résume en points clés analytiques les échanges suivants entre un avocat et l'IA sur un dossier juridique.
                Extrais uniquement les informations factuelles, les clarifications importantes et les observations de l'avocat.
                Ignore les reformulations et questions triviales. Sois concis (maximum 10 points).
                """;

        try {
            AnthropicResult result = anthropicService.analyzeFast(systemPrompt, chatText, 512);
            String summary = result.content();
            return (summary != null && !summary.isBlank()) ? summary.trim() : null;
        } catch (Exception e) {
            log.warn("Chat summary failed for caseFile {} — enriched analysis will proceed without it", caseFileId, e);
            return null;
        }
    }

    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult, String chatSummary,
                                List<String> nonCompliantChecks, List<String> toCheckChecks) {
        List<AiQuestion> questions = aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId);

        List<AiQuestion> answeredQuestions = questions.stream()
                .filter(q -> "ANSWERED".equals(q.getStatus()))
                .toList();

        String qaSection = IntStream.range(0, answeredQuestions.size())
                .mapToObj(i -> {
                    AiQuestion q = answeredQuestions.get(i);
                    String answerText = aiQuestionAnswerRepository
                            .findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())
                            .map(AiQuestionAnswer::getAnswerText)
                            .orElse("(sans réponse)");
                    return "Q%d : %s\nR%d : %s".formatted(i + 1, q.getQuestionText(), i + 1, answerText);
                })
                .collect(Collectors.joining("\n"));

        StringBuilder prompt = new StringBuilder();
        prompt.append("[Synthèse précédente]\n").append(previousAnalysisResult).append("\n\n");
        prompt.append("[Questions et réponses de l'avocat]\n")
              .append(qaSection.isEmpty() ? "(aucune réponse)" : qaSection);

        if (chatSummary != null && !chatSummary.isBlank()) {
            prompt.append("\n\n[Échanges libres avec l'assistant — points clés]\n").append(chatSummary);
        }

        if (nonCompliantChecks != null && !nonCompliantChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux non conformes]\n");
            nonCompliantChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        if (toCheckChecks != null && !toCheckChecks.isEmpty()) {
            prompt.append("\n\n[Points procéduraux à vérifier — non encore qualifiés par l'avocat]\n");
            toCheckChecks.forEach(c -> prompt.append("- ").append(c).append("\n"));
        }

        return prompt.toString();
    }
}
