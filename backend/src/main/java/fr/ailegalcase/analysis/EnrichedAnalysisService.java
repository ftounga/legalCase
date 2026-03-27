package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
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
            Format attendu : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            """;

    static String buildSystemPrompt(String legalDomain, String country) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(LegalDomainPromptBuilder.domainLabel(legalDomain, country));
    }

    record PreparedEnrichedAnalysis(UUID analysisId, String prompt, String systemPrompt, UUID caseFileId) {}

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
                                   AnalysisQaSnapshotService analysisQaSnapshotService) {
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

        self.finalizeEnrichedAnalysis(prepared.analysisId(), prepared.caseFileId(), result, failure);
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
        String systemPrompt = buildSystemPrompt(
                ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL",
                ws != null ? ws.getCountry() : "FRANCE");
        String prompt = buildEnrichedPrompt(caseFileId, previousAnalysis.getAnalysisResult());
        return new PreparedEnrichedAnalysis(enrichedAnalysis.getId(), prompt, systemPrompt, caseFileId);
    }

    @Transactional
    public void finalizeEnrichedAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure) {
        CaseAnalysis enrichedAnalysis = caseAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            enrichedAnalysis.setAnalysisResult(AnalysisJsonTruncator.truncateCaseAnalysis(result.content()));
            enrichedAnalysis.setModelUsed(result.modelUsed());
            enrichedAnalysis.setPromptTokens(result.promptTokens());
            enrichedAnalysis.setCompletionTokens(result.completionTokens());
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
        }
        caseAnalysisRepository.save(enrichedAnalysis);

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

    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult) {
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

        return """
                [Synthèse précédente]
                %s

                [Questions et réponses de l'avocat]
                %s
                """.formatted(previousAnalysisResult, qaSection.isEmpty() ? "(aucune réponse)" : qaSection);
    }
}
