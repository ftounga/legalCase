package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Profile({"local", "prod"})
public class DocumentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisService.class);

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert en droit du travail français.
            Tu reçois les analyses de plusieurs segments d'un document juridique.
            Produis une synthèse globale du document en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            Contraintes de longueur : 5 faits maximum, 3 points_juridiques maximum, 3 risques maximum, 3 questions_ouvertes maximum. Sois concis.
            """;

    private final ChunkAnalysisRepository chunkAnalysisRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final DocumentRepository documentRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final UsageEventService usageEventService;
    private final CaseFileRepository caseFileRepository;

    public DocumentAnalysisService(ChunkAnalysisRepository chunkAnalysisRepository,
                                   DocumentAnalysisRepository documentAnalysisRepository,
                                   DocumentExtractionRepository extractionRepository,
                                   DocumentRepository documentRepository,
                                   AnthropicService anthropicService,
                                   AnalysisJobRepository analysisJobRepository,
                                   UsageEventService usageEventService,
                                   CaseFileRepository caseFileRepository) {
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.extractionRepository = extractionRepository;
        this.documentRepository = documentRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.usageEventService = usageEventService;
        this.caseFileRepository = caseFileRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_ANALYSIS_QUEUE)
    @Transactional
    public void consumeDocumentAnalysis(DocumentAnalysisMessage message) {
        UUID extractionId = message.extractionId();

        List<ChunkAnalysis> chunkAnalyses = chunkAnalysisRepository
                .findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE);

        if (chunkAnalyses.isEmpty()) {
            log.warn("No DONE chunk analyses found for extraction {} — document analysis skipped", extractionId);
            return;
        }

        DocumentExtraction extraction = extractionRepository.findById(extractionId).orElse(null);
        if (extraction == null) {
            log.error("Extraction {} not found — document analysis skipped", extractionId);
            return;
        }

        UUID caseFileId = extractionRepository.findCaseFileIdById(extractionId).orElse(null);
        createOrResetDocumentAnalysisJob(caseFileId);

        DocumentAnalysis analysis = new DocumentAnalysis();
        analysis.setDocument(extraction.getDocument());
        analysis.setExtraction(extraction);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = documentAnalysisRepository.save(analysis);

        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = documentAnalysisRepository.save(analysis);

        try {
            String aggregatedPrompt = buildAggregatedPrompt(chunkAnalyses);
            AnthropicResult result = anthropicService.analyzeFast(SYSTEM_PROMPT, aggregatedPrompt, 4096);
            analysis.setAnalysisResult(AnalysisJsonTruncator.truncateDocumentAnalysis(result.content()));
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            log.info("Document analysis DONE for extraction {}", extractionId);
        } catch (Exception e) {
            log.error("Document analysis FAILED for extraction {}", extractionId, e);
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }

        documentAnalysisRepository.save(analysis);

        if (analysis.getAnalysisStatus() == AnalysisStatus.DONE) {
            updateDocumentAnalysisJob(caseFileId);
            if (caseFileId != null) {
                int promptTokens = analysis.getPromptTokens();
                int completionTokens = analysis.getCompletionTokens();
                caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                    usageEventService.record(caseFileId, userId, JobType.DOCUMENT_ANALYSIS,
                            promptTokens, completionTokens));
            }
        }
    }

    private void createOrResetDocumentAnalysisJob(UUID caseFileId) {
        if (caseFileId == null) return;
        long totalDocs = documentRepository.countByCaseFileId(caseFileId);
        analysisJobRepository.upsertDocumentAnalysisJob(caseFileId, (int) totalDocs);
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
