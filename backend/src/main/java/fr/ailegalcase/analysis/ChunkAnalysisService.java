package fr.ailegalcase.analysis;

import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.ChunkingDoneEvent;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.shared.PaymentRequiredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Service
@Profile({"local", "prod"})
public class ChunkAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ChunkAnalysisService.class);

    private final RabbitTemplate rabbitTemplate;
    private final DocumentChunkRepository chunkRepository;
    private final ChunkAnalysisRepository analysisRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final UsageEventService usageEventService;
    private final CaseFileRepository caseFileRepository;
    private final PlanLimitService planLimitService;

    public ChunkAnalysisService(RabbitTemplate rabbitTemplate,
                                DocumentChunkRepository chunkRepository,
                                ChunkAnalysisRepository analysisRepository,
                                DocumentAnalysisRepository documentAnalysisRepository,
                                AnthropicService anthropicService,
                                AnalysisJobRepository analysisJobRepository,
                                DocumentExtractionRepository extractionRepository,
                                UsageEventService usageEventService,
                                CaseFileRepository caseFileRepository,
                                PlanLimitService planLimitService) {
        this.rabbitTemplate = rabbitTemplate;
        this.chunkRepository = chunkRepository;
        this.analysisRepository = analysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.extractionRepository = extractionRepository;
        this.usageEventService = usageEventService;
        this.caseFileRepository = caseFileRepository;
        this.planLimitService = planLimitService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChunkingDone(ChunkingDoneEvent event) {
        log.debug("Publishing {} chunk analysis messages for extraction {}",
                event.chunkIds().size(), event.extractionId());

        UUID caseFileId = extractionRepository.findCaseFileIdById(event.extractionId()).orElse(null);

        // Pre-fetch context once for all chunks of this extraction — eliminates 6 DB lookups per chunk consumer
        CaseFileContext context = caseFileId != null
                ? caseFileRepository.findContextById(caseFileId).orElse(null)
                : null;

        for (UUID chunkId : event.chunkIds()) {
            ChunkAnalysisMessage msg = context != null
                    ? new ChunkAnalysisMessage(chunkId, caseFileId, context.workspaceId(),
                            context.legalDomain(), context.country(), context.userId())
                    : ChunkAnalysisMessage.forChunk(chunkId);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHUNK_ANALYSIS_EXCHANGE,
                    RabbitMQConfig.CHUNK_ANALYSIS_ROUTING_KEY,
                    msg
            );
        }

        if (caseFileId == null) return;

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CHUNK_ANALYSIS);
                    j.setStatus(AnalysisStatus.PENDING);
                    j.setTotalItems(0);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setTotalItems(job.getTotalItems() + event.chunkIds().size());
        analysisJobRepository.save(job);
    }

    @RabbitListener(queues = RabbitMQConfig.CHUNK_ANALYSIS_QUEUE, concurrency = "5")
    public void consumeChunkAnalysis(ChunkAnalysisMessage message) {
        UUID chunkId = message.chunkId();
        long startMs = System.currentTimeMillis();

        DocumentChunk chunk = chunkRepository.findById(chunkId).orElse(null);
        if (chunk == null) {
            log.error("Chunk {} not found — analysis skipped", chunkId);
            return;
        }

        if (chunk.getChunkText() == null || chunk.getChunkText().isBlank()) {
            log.warn("Chunk {} has empty text — analysis skipped", chunkId);
            return;
        }

        // Use pre-fetched context from message if available, fall back to DB (backward compat)
        UUID caseFileId = message.caseFileId() != null
                ? message.caseFileId()
                : (chunk.getExtraction() != null
                        ? extractionRepository.findCaseFileIdById(chunk.getExtraction().getId()).orElse(null)
                        : null);

        UUID workspaceId = message.workspaceId() != null
                ? message.workspaceId()
                : (caseFileId != null ? caseFileRepository.findWorkspaceIdById(caseFileId).orElse(null) : null);

        if (workspaceId != null && planLimitService.isMonthlyTokenBudgetExceeded(workspaceId)) {
            log.warn("Monthly token budget exceeded for workspace {} — chunk {} skipped", workspaceId, chunkId);
            ChunkAnalysis skipped = new ChunkAnalysis();
            skipped.setChunk(chunk);
            skipped.setAnalysisStatus(AnalysisStatus.SKIPPED);
            analysisRepository.save(skipped);
            return;
        }

        ChunkAnalysis analysis = new ChunkAnalysis();
        analysis.setChunk(chunk);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = analysisRepository.save(analysis);

        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = analysisRepository.save(analysis);

        String legalDomain = message.legalDomain() != null
                ? message.legalDomain()
                : (caseFileId != null
                        ? caseFileRepository.findLegalDomainById(caseFileId).orElse("DROIT_DU_TRAVAIL")
                        : "DROIT_DU_TRAVAIL");
        String country = message.country() != null
                ? message.country()
                : (caseFileId != null
                        ? caseFileRepository.findCountryById(caseFileId).orElse("FRANCE")
                        : "FRANCE");

        // F-257 — résolution userId avant l'appel (obligatoire pour AiCallContext user-level).
        // L'ancienne logique de fallback DB est conservée.
        UUID resolvedUserId = message.userId() != null
                ? message.userId()
                : (caseFileId != null
                        ? caseFileRepository.findCreatedByUserIdById(caseFileId).orElse(null)
                        : null);

        if (resolvedUserId == null || workspaceId == null || caseFileId == null) {
            log.warn("Chunk {} missing user/workspace/caseFile context (userId={}, workspaceId={}, caseFileId={}) — analysis skipped",
                    chunkId, resolvedUserId, workspaceId, caseFileId);
            analysis.setAnalysisStatus(AnalysisStatus.SKIPPED);
            analysisRepository.save(analysis);
            return;
        }

        try {
            log.info("Chunk analysis START for chunk {} ({} chars)", chunkId, chunk.getChunkText().length());
            long anthropicStart = System.currentTimeMillis();
            AiCallContext ctx = AiCallContext.userLevel(workspaceId, resolvedUserId, caseFileId, JobType.CHUNK_ANALYSIS);
            AnthropicResult result = anthropicService.analyzeChunk(ctx, chunk.getChunkText(), legalDomain, country);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            analysis.setAnalysisResult(result.content());
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            log.info("Chunk analysis DONE for chunk {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    chunkId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (PaymentRequiredException pre) {
            // F-257 — quota dépassé pendant l'analyse (cas race après le pre-check ligne 127) → SKIPPED.
            log.warn("Token budget exceeded during chunk {} analysis — chunk skipped (workspace={})",
                    chunkId, workspaceId);
            analysis.setAnalysisStatus(AnalysisStatus.SKIPPED);
        } catch (Exception e) {
            log.error("Chunk analysis FAILED for chunk {} (total {}ms)", chunkId,
                    System.currentTimeMillis() - startMs, e);
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }

        analysisRepository.save(analysis);

        // F-147 SF-147-01 : si l'analyse chunk a échoué, marquer aussi le job
        // CHUNK_ANALYSIS en FAILED. Sinon il reste en PROCESSING à l'infini.
        if (analysis.getAnalysisStatus() == AnalysisStatus.FAILED && caseFileId != null) {
            markChunkJobFailed(caseFileId);
        }

        if (analysis.getAnalysisStatus() == AnalysisStatus.DONE) {
            UUID extractionId = chunk.getExtraction().getId();
            updateChunkJob(extractionId, caseFileId);
            triggerDocumentAnalysisIfReady(extractionId);
            // F-257 — record automatique dans AnthropicService.doAnalyze, plus de record manuel ici.
        }
    }

    /**
     * F-147 SF-147-01 : marque le job CHUNK_ANALYSIS en FAILED quand un chunk
     * échoue (Anthropic down, rate limit, 400…). Sans ce correctif le job
     * reste en PROCESSING et bloque la suppression du case file.
     */
    private void markChunkJobFailed(UUID caseFileId) {
        analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS).ifPresent(job -> {
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Chunk analysis failed");
            analysisJobRepository.save(job);
        });
    }

    private void updateChunkJob(UUID extractionId, UUID caseFileId) {
        if (caseFileId == null) {
            caseFileId = extractionRepository.findCaseFileIdById(extractionId).orElse(null);
        }
        if (caseFileId == null) return;

        final UUID resolvedCaseFileId = caseFileId;
        analysisJobRepository.findByCaseFileIdAndJobType(resolvedCaseFileId, JobType.CHUNK_ANALYSIS).ifPresent(job -> {
            long done = analysisRepository.countByChunkExtractionDocumentCaseFileIdAndAnalysisStatus(
                    resolvedCaseFileId, AnalysisStatus.DONE);
            job.setProcessedItems((int) done);
            if (job.getTotalItems() > 0 && done >= job.getTotalItems()) {
                job.setStatus(AnalysisStatus.DONE);
            }
            analysisJobRepository.save(job);
        });
    }

    private void triggerDocumentAnalysisIfReady(UUID extractionId) {
        long totalChunks = chunkRepository.countByExtractionId(extractionId);
        long doneAnalyses = analysisRepository.countByChunkExtractionIdAndAnalysisStatus(
                extractionId, AnalysisStatus.DONE);

        if (totalChunks != doneAnalyses) {
            return;
        }

        boolean alreadyTriggered = documentAnalysisRepository.existsByExtractionIdAndAnalysisStatusIn(
                extractionId, List.of(AnalysisStatus.PENDING, AnalysisStatus.PROCESSING, AnalysisStatus.DONE));

        if (alreadyTriggered) {
            log.debug("DocumentAnalysis already exists for extraction {} — skipping", extractionId);
            return;
        }

        log.info("All chunks DONE for extraction {} — triggering document analysis", extractionId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DOCUMENT_ANALYSIS_EXCHANGE,
                RabbitMQConfig.DOCUMENT_ANALYSIS_ROUTING_KEY,
                new DocumentAnalysisMessage(extractionId, false)
        );
    }
}
