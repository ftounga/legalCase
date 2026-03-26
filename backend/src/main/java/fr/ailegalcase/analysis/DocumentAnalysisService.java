package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
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
            Contraintes de longueur : 5 faits maximum, 3 points_juridiques maximum, 3 risques maximum, 3 questions_ouvertes maximum. Sois concis.
            """;

    static String buildSystemPrompt(String legalDomain, String country) {
        return SYSTEM_PROMPT_TEMPLATE.formatted(LegalDomainPromptBuilder.domainLabel(legalDomain, country));
    }

    record PreparedAnalysis(DocumentAnalysis analysis, String prompt, String systemPrompt, UUID caseFileId) {}

    private final ChunkAnalysisRepository chunkAnalysisRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final DocumentRepository documentRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final UsageEventService usageEventService;
    private final CaseFileRepository caseFileRepository;
    private final ApplicationEventPublisher eventPublisher;

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
                                   ApplicationEventPublisher eventPublisher) {
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.extractionRepository = extractionRepository;
        this.documentRepository = documentRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.usageEventService = usageEventService;
        this.caseFileRepository = caseFileRepository;
        this.eventPublisher = eventPublisher;
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

        self.finalizeAnalysis(prepared.analysis().getId(), prepared.caseFileId(), result, failure);
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

        DocumentAnalysis analysis = new DocumentAnalysis();
        analysis.setDocument(extraction.getDocument());
        analysis.setExtraction(extraction);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = documentAnalysisRepository.save(analysis);
        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = documentAnalysisRepository.save(analysis);

        return new PreparedAnalysis(analysis, prompt, buildSystemPrompt(legalDomain, country), caseFileId);
    }

    @Transactional
    public void finalizeAnalysis(UUID analysisId, UUID caseFileId, AnthropicResult result, Exception failure) {
        DocumentAnalysis analysis = documentAnalysisRepository.findById(analysisId).orElseThrow();

        if (failure != null) {
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        } else {
            analysis.setAnalysisResult(AnalysisJsonTruncator.truncateDocumentAnalysis(result.content()));
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
        }
        documentAnalysisRepository.save(analysis);

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
