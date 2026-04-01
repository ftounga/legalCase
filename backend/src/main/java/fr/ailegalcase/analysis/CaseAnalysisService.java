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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
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
public class CaseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisService.class);

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois les analyses de plusieurs documents d'un dossier juridique.
            Produis une synthèse globale du dossier en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...], "pieces_manquantes": [...], "points_procedure": [...]}
            La timeline doit lister les événements clés du dossier par ordre chronologique. Si aucune date n'est identifiable, utilise "timeline": [].
            Le champ "pieces_manquantes" liste les pièces habituellement attendues dans ce type de dossier qui sont absentes des documents fournis (ex: "Contrat de travail", "Bulletins de salaire"). Si le dossier semble complet, utilise "pieces_manquantes": [].
            Le champ "points_procedure" liste les étapes procédurales légalement requises dans ce type de dossier (ex: "Entretien préalable tenu dans les délais", "Lettre de licenciement motivée"). Si la procédure semble conforme, utilise "points_procedure": [].
            Contraintes de longueur : %d entrées timeline maximum, %d faits maximum, %d points_juridiques maximum, %d risques maximum, %d questions_ouvertes maximum, %d pièces manquantes maximum, %d points procédure maximum. Sois concis.
            """;

    static String buildSystemPrompt(String legalDomain, String country, AnalysisLimitsProperties.LevelLimits limits) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country),
                limits.getTimeline(), limits.getFaits(),
                limits.getPointsJuridiques(), limits.getRisques(), limits.getQuestionsOuvertes(),
                limits.getPiecesManquantes(), limits.getPointsProcedure());
    }

    record PreparedCaseAnalysis(UUID analysisId, String prompt, String systemPrompt, UUID caseFileId,
                                 AnalysisLimitsProperties.LevelLimits limits) {}

    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService;
    private final AnalysisLimitsProperties analysisLimitsProperties;
    private final ProcedureCheckService procedureCheckService;

    @Lazy @Autowired
    private CaseAnalysisService self;

    public CaseAnalysisService(DocumentAnalysisRepository documentAnalysisRepository,
                               CaseAnalysisRepository caseAnalysisRepository,
                               CaseFileRepository caseFileRepository,
                               AnthropicService anthropicService,
                               AnalysisJobRepository analysisJobRepository,
                               RabbitTemplate rabbitTemplate,
                               UsageEventService usageEventService,
                               ApplicationEventPublisher eventPublisher,
                               AnalysisDocumentSnapshotService analysisDocumentSnapshotService,
                               AnalysisLimitsProperties analysisLimitsProperties,
                               ProcedureCheckService procedureCheckService) {
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
        this.analysisDocumentSnapshotService = analysisDocumentSnapshotService;
        this.analysisLimitsProperties = analysisLimitsProperties;
        this.procedureCheckService = procedureCheckService;
    }

    @RabbitListener(queues = RabbitMQConfig.CASE_ANALYSIS_QUEUE, concurrency = "3")
    public void consumeCaseAnalysis(CaseAnalysisMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedCaseAnalysis prepared = self.prepareCaseAnalysis(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Case analysis START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            result = anthropicService.analyze(prepared.systemPrompt(), prepared.prompt(), 8192);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Case analysis DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Case analysis FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeCaseAnalysis(prepared.analysisId(), prepared.caseFileId(), result, failure, prepared.limits());
    }

    @Transactional
    public PreparedCaseAnalysis prepareCaseAnalysis(CaseAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        List<DocumentAnalysis> documentAnalyses = documentAnalysisRepository
                .findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE);

        if (documentAnalyses.isEmpty()) {
            log.warn("No DONE document analyses found for caseFile {} — case analysis skipped", caseFileId);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — case analysis skipped", caseFileId);
            return null;
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setVersion(nextVersion);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = caseAnalysisRepository.save(analysis);
        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = caseAnalysisRepository.save(analysis);

        analysisDocumentSnapshotService.snapshot(analysis.getId(), caseFile);

        fr.ailegalcase.workspace.Workspace ws = caseFile.getWorkspace();
        String legalDomain = ws != null ? ws.getLegalDomain() : "DROIT_DU_TRAVAIL";
        String country     = ws != null ? ws.getCountry()     : "FRANCE";
        AnalysisLimitsProperties.LevelLimits limits = analysisLimitsProperties.forDomain(legalDomain).getDossier();
        String systemPrompt = buildSystemPrompt(legalDomain, country, limits);
        return new PreparedCaseAnalysis(analysis.getId(), buildAggregatedPrompt(documentAnalyses), systemPrompt, caseFileId, limits);
    }

    @Transactional
    public void finalizeCaseAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure,
                                      AnalysisLimitsProperties.LevelLimits limits) {
        CaseAnalysis analysis = caseAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            String truncated = AnalysisJsonTruncator.truncateCaseAnalysis(result.content(), limits);
            analysis.setAnalysisResult(truncated);
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            CaseAnalysisResponse.populateCounts(analysis, truncated);
        }
        caseAnalysisRepository.save(analysis);

        if (failure == null) {
            procedureCheckService.createChecks(analysis, analysis.getAnalysisResult());
        }

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setProcessedItems(1);
        job.setStatus(analysis.getAnalysisStatus());
        if (analysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Case analysis failed");
            reportJobFailureToSentry(caseFileId, JobType.CASE_ANALYSIS, "Case analysis failed");
        }
        analysisJobRepository.save(job);

        AnalysisStatus finalStatus = analysis.getAnalysisStatus();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, finalStatus, JobType.CASE_ANALYSIS));
                if (finalStatus == AnalysisStatus.DONE) {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.AI_QUESTION_GENERATION_EXCHANGE,
                            RabbitMQConfig.AI_QUESTION_GENERATION_ROUTING_KEY,
                            new AiQuestionGenerationMessage(caseFileId));
                }
            }
        });

        if (finalStatus == AnalysisStatus.DONE) {
            int promptTokens = analysis.getPromptTokens();
            int completionTokens = analysis.getCompletionTokens();
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.CASE_ANALYSIS,
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

    private String buildAggregatedPrompt(List<DocumentAnalysis> documentAnalyses) {
        List<DocumentAnalysis> sorted = documentAnalyses.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();
        return IntStream.range(0, sorted.size())
                .mapToObj(i -> "Document %d : %s".formatted(i, sorted.get(i).getAnalysisResult()))
                .collect(Collectors.joining("\n"));
    }
}
