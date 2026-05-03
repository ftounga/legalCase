package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
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

@Service
@Profile({"local", "prod"})
public class DocumentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisService.class);

    static final String SYSTEM_PROMPT_TEMPLATE = """
            Tu es un assistant juridique expert en %s.
            Tu reçois les analyses de plusieurs segments d'un document juridique.
            Produis une synthèse globale du document en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            Contraintes de longueur : %d faits maximum, %d points_juridiques maximum, %d risques maximum, %d questions_ouvertes maximum. Sois concis.
            """;

    static String buildSystemPrompt(String legalDomain, String country, AnalysisLimitsProperties.LevelLimits limits) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(
                LegalDomainPromptBuilder.domainLabel(legalDomain, country),
                limits.getFaits(), limits.getPointsJuridiques(),
                limits.getRisques(), limits.getQuestionsOuvertes());
    }

    record PreparedAnalysis(DocumentAnalysis analysis, String prompt, String systemPrompt, UUID caseFileId,
                             AnalysisLimitsProperties.LevelLimits limits) {}

    private final ChunkAnalysisRepository chunkAnalysisRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final DocumentRepository documentRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final UsageEventService usageEventService;
    private final CaseFileRepository caseFileRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AnalysisLimitsProperties analysisLimitsProperties;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final RabbitTemplate rabbitTemplate;

    @Lazy @Autowired
    private DocumentAnalysisService self;

    public DocumentAnalysisService(ChunkAnalysisRepository chunkAnalysisRepository,
                                   DocumentAnalysisRepository documentAnalysisRepository,
                                   DocumentExtractionRepository extractionRepository,
                                   DocumentRepository documentRepository,
                                   AnthropicService anthropicService,
                                   AnalysisJobRepository analysisJobRepository,
                                   UsageEventService usageEventService,
                                   CaseFileRepository caseFileRepository,
                                   ApplicationEventPublisher eventPublisher,
                                   AnalysisLimitsProperties analysisLimitsProperties,
                                   CaseAnalysisRepository caseAnalysisRepository,
                                   RabbitTemplate rabbitTemplate) {
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.extractionRepository = extractionRepository;
        this.documentRepository = documentRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.usageEventService = usageEventService;
        this.caseFileRepository = caseFileRepository;
        this.eventPublisher = eventPublisher;
        this.analysisLimitsProperties = analysisLimitsProperties;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_ANALYSIS_QUEUE, concurrency = "5")
    public void consumeDocumentAnalysis(DocumentAnalysisMessage message) {
        long startMs = System.currentTimeMillis();
        UUID extractionId = message.extractionId();

        PreparedAnalysis prepared = self.prepareAnalysis(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Document analysis START for extraction {} ({}, {} chars)",
                    extractionId, message.directAnalysis() ? "direct" : "chunked", prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            result = anthropicService.analyzeFast(prepared.systemPrompt(), prepared.prompt(), 4096);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Document analysis DONE for extraction {} ({}) — Anthropic {}ms, total {}ms, tokens {}/{}",
                    extractionId, message.directAnalysis() ? "direct" : "chunked",
                    anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Document analysis FAILED for extraction {} (total {}ms)", extractionId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeAnalysis(prepared.analysis().getId(), prepared.caseFileId(), result, failure, prepared.limits());
    }

    @Transactional
    public PreparedAnalysis prepareAnalysis(DocumentAnalysisMessage message) {
        UUID extractionId = message.extractionId();

        List<ChunkAnalysis> chunkAnalyses = message.directAnalysis()
                ? List.of()
                : chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE);

        if (!message.directAnalysis() && chunkAnalyses.isEmpty()) {
            log.warn("No DONE chunk analyses found for extraction {} — document analysis skipped", extractionId);
            return null;
        }

        DocumentExtraction extraction = extractionRepository.findById(extractionId).orElse(null);
        if (extraction == null) {
            log.error("Extraction {} not found — document analysis skipped", extractionId);
            return null;
        }

        UUID caseFileId = extractionRepository.findCaseFileIdById(extractionId).orElse(null);
        createOrResetDocumentAnalysisJob(caseFileId);

        String legalDomain = caseFileId != null
                ? caseFileRepository.findLegalDomainById(caseFileId).orElse("DROIT_DU_TRAVAIL")
                : "DROIT_DU_TRAVAIL";
        String country = caseFileId != null
                ? caseFileRepository.findCountryById(caseFileId).orElse("FRANCE")
                : "FRANCE";

        String prompt = message.directAnalysis()
                ? extraction.getExtractedText()
                : buildAggregatedPrompt(chunkAnalyses);

        AnalysisLimitsProperties.LevelLimits limits = analysisLimitsProperties.forDomain(legalDomain).getDocument();

        DocumentAnalysis analysis = new DocumentAnalysis();
        analysis.setDocument(extraction.getDocument());
        analysis.setExtraction(extraction);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = documentAnalysisRepository.save(analysis);
        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = documentAnalysisRepository.save(analysis);

        return new PreparedAnalysis(analysis, prompt, buildSystemPrompt(legalDomain, country, limits), caseFileId, limits);
    }

    @Transactional
    public void finalizeAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure,
                                  AnalysisLimitsProperties.LevelLimits limits) {
        DocumentAnalysis analysis = documentAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            analysis.setAnalysisResult(AnalysisJsonTruncator.truncateDocumentAnalysis(result.content(), limits));
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
        }
        documentAnalysisRepository.save(analysis);

        // F-147 SF-147-01 : si l'analyse a échoué, marquer aussi le job FAILED
        // sinon il reste en PROCESSING à l'infini et bloque la suppression du
        // case file (cf. CaseFileStatusService.isPipelineActive).
        if (failure != null && caseFileId != null) {
            markDocumentAnalysisJobFailed(caseFileId, failure);
        }

        if (analysis.getAnalysisStatus() == AnalysisStatus.DONE) {
            updateDocumentAnalysisJob(caseFileId);
            if (caseFileId != null) {
                boolean allDocsDone = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS)
                        .map(j -> j.getStatus() == AnalysisStatus.DONE)
                        .orElse(false);
                if (allDocsDone) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishEvent(new AnalysisStatusEvent(
                                    caseFileId, AnalysisStatus.DONE, JobType.DOCUMENT_ANALYSIS));
                        }
                    });
                }
                int promptTokens = analysis.getPromptTokens();
                int completionTokens = analysis.getCompletionTokens();
                caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                    usageEventService.record(caseFileId, userId, JobType.DOCUMENT_ANALYSIS,
                            promptTokens, completionTokens));

                // F-185 SF-185-03 — synthèse incrémentale par doc.
                // À chaque DocumentAnalysis DONE, déclencher une CaseAnalysis provisoire
                // pour donner à l'avocat une 1re vision dès le 1er document analysé,
                // enrichie au fil des docs suivants. Garde-fous (skipIfAlreadyRunning)
                // dans triggerProvisionalCaseAnalysisAfterCommit pour éviter le spam.
                triggerProvisionalCaseAnalysisAfterCommit(caseFileId);
            }
        }
    }

    /**
     * F-185 SF-185-03 — déclenche une CaseAnalysis provisoire (auto, vs manuelle)
     * après commit, sous garde-fous :
     *   - skip si une CaseAnalysis est déjà PENDING/PROCESSING/PARTIAL (race avec
     *     un autre doc qui vient d'arriver, ou avec un déclenchement manuel)
     *   - skip si la dernière analyse DONE est non-provisoire (l'avocat a tranché
     *     manuellement sur le périmètre courant — on ne lui repasse pas dessus)
     *
     * Le RabbitMQ guarantee at-least-once : si plusieurs docs DONE arrivent dans
     * la même fenêtre, plusieurs messages peuvent être publiés ; le check
     * existsByCaseFileIdAndAnalysisStatusIn dans prepareCaseAnalysis fait office
     * de seconde barrière (la 2e Prepare verra l'analyse précédente PROCESSING
     * et créera une nouvelle version, ce qui est OK).
     */
    private void triggerProvisionalCaseAnalysisAfterCommit(UUID caseFileId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    boolean alreadyRunning = caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(
                            caseFileId, List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING, AnalysisStatus.PARTIAL));
                    if (alreadyRunning) {
                        log.debug("Skipping provisional case analysis for {} — analysis already in-flight", caseFileId);
                        return;
                    }
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.CASE_ANALYSIS_EXCHANGE,
                            RabbitMQConfig.CASE_ANALYSIS_ROUTING_KEY,
                            new CaseAnalysisMessage(caseFileId, true));
                    log.info("Provisional case analysis triggered for {} after document analysis DONE", caseFileId);
                } catch (Exception ex) {
                    // Fail-open : pas critique si on n'arrive pas à déclencher la provisoire,
                    // l'avocat peut toujours déclencher manuellement.
                    log.warn("Failed to trigger provisional case analysis for {}: {}",
                            caseFileId, ex.getMessage());
                }
            }
        });
    }

    private void createOrResetDocumentAnalysisJob(UUID caseFileId) {
        if (caseFileId == null) return;
        long totalDocs = documentRepository.countByCaseFileId(caseFileId);
        analysisJobRepository.upsertDocumentAnalysisJob(caseFileId, (int) totalDocs);
    }

    /**
     * F-147 SF-147-01 : marque le job DOCUMENT_ANALYSIS en FAILED quand une
     * analyse individuelle échoue (Anthropic 400/429/5xx, timeout…). Sans ce
     * correctif, le job reste en PROCESSING à l'infini et bloque la
     * suppression du case file via {@code isPipelineActive}.
     */
    private void markDocumentAnalysisJobFailed(UUID caseFileId, Exception failure) {
        analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS).ifPresent(job -> {
            job.setStatus(AnalysisStatus.FAILED);
            String msg = failure.getMessage() != null ? failure.getMessage() : failure.getClass().getSimpleName();
            job.setErrorMessage(msg.length() > 500 ? msg.substring(0, 500) : msg);
            analysisJobRepository.save(job);
        });
    }

    private void updateDocumentAnalysisJob(UUID caseFileId) {
        if (caseFileId == null) return;

        analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS).ifPresent(job -> {
            long done = documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(
                    caseFileId, AnalysisStatus.DONE);
            // clamp to totalItems to prevent progressPercentage > 100 under race conditions
            int clamped = (int) Math.min(done, job.getTotalItems());
            job.setProcessedItems(clamped);
            if (job.getTotalItems() > 0 && done >= job.getTotalItems()) {
                job.setStatus(AnalysisStatus.DONE);
            }
            analysisJobRepository.save(job);
        });
    }

    private String buildAggregatedPrompt(List<ChunkAnalysis> chunkAnalyses) {
        return chunkAnalyses.stream()
                .sorted((a, b) -> a.getChunk().getChunkIndex() - b.getChunk().getChunkIndex())
                .map(ca -> "Chunk %d : %s".formatted(ca.getChunk().getChunkIndex(), ca.getAnalysisResult()))
                .collect(Collectors.joining("\n"));
    }
}
