package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.ChunkingDoneEvent;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtractionRepository;
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
@Profile("local")
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

    public ChunkAnalysisService(RabbitTemplate rabbitTemplate,
                                DocumentChunkRepository chunkRepository,
                                ChunkAnalysisRepository analysisRepository,
                                DocumentAnalysisRepository documentAnalysisRepository,
                                AnthropicService anthropicService,
                                AnalysisJobRepository analysisJobRepository,
                                DocumentExtractionRepository extractionRepository,
                                UsageEventService usageEventService,
                                CaseFileRepository caseFileRepository) {
        this.rabbitTemplate = rabbitTemplate;
        this.chunkRepository = chunkRepository;
        this.analysisRepository = analysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.extractionRepository = extractionRepository;
        this.usageEventService = usageEventService;
        this.caseFileRepository = caseFileRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChunkingDone(ChunkingDoneEvent event) {
        log.debug("Publishing {} chunk analysis messages for extraction {}",
                event.chunkIds().size(), event.extractionId());
        for (UUID chunkId : event.chunkIds()) {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHUNK_ANALYSIS_EXCHANGE,
                    RabbitMQConfig.CHUNK_ANALYSIS_ROUTING_KEY,
                    new ChunkAnalysisMessage(chunkId)
            );
        }

        UUID caseFileId = extractionRepository.findCaseFileIdById(event.extractionId()).orElse(null);
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

    @RabbitListener(queues = RabbitMQConfig.CHUNK_ANALYSIS_QUEUE, concurrency = "3")
    public void consumeChunkAnalysis(ChunkAnalysisMessage message) {
        UUID chunkId = message.chunkId();

        DocumentChunk chunk = chunkRepository.findById(chunkId).orElse(null);
        if (chunk == null) {
            log.error("Chunk {} not found — analysis skipped", chunkId);
            return;
        }

        if (chunk.getChunkText() == null || chunk.getChunkText().isBlank()) {
            log.warn("Chunk {} has empty text — analysis skipped", chunkId);
            return;
        }

        ChunkAnalysis analysis = new ChunkAnalysis();
        analysis.setChunk(chunk);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = analysisRepository.save(analysis);

        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = analysisRepository.save(analysis);

        try {
            AnthropicResult result = anthropicService.analyzeChunk(chunk.getChunkText());
            analysis.setAnalysisResult(result.content());
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            log.info("Analysis DONE for chunk {}", chunkId);
        } catch (Exception e) {
            log.error("Analysis FAILED for chunk {}", chunkId, e);
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }

        analysisRepository.save(analysis);

        if (analysis.getAnalysisStatus() == AnalysisStatus.DONE) {
            UUID extractionId = chunk.getExtraction().getId();
            updateChunkJob(extractionId);
            triggerDocumentAnalysisIfReady(extractionId);
            int promptTokens = analysis.getPromptTokens();
            int completionTokens = analysis.getCompletionTokens();
            extractionRepository.findCaseFileIdById(extractionId).ifPresent(caseFileId ->
                caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                    usageEventService.record(caseFileId, userId, JobType.CHUNK_ANALYSIS,
                            promptTokens, completionTokens)));
        }
    }

    private void updateChunkJob(UUID extractionId) {
        UUID caseFileId = extractionRepository.findCaseFileIdById(extractionId).orElse(null);
        if (caseFileId == null) return;

        analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS).ifPresent(job -> {
            long done = analysisRepository.countByChunkExtractionDocumentCaseFileIdAndAnalysisStatus(
                    caseFileId, AnalysisStatus.DONE);
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
                new DocumentAnalysisMessage(extractionId)
        );
    }
}
