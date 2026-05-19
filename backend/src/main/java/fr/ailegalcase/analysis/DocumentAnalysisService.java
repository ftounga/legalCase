package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.document.ExtractionFailedEvent;
import fr.ailegalcase.document.ExtractionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
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
                                   AnalysisLimitsProperties analysisLimitsProperties) {
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
            recomputeDocumentAnalysisJobCompletion(caseFileId);
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

    /**
     * SF-121-05 : ré-évalue la complétion du job DOCUMENT_ANALYSIS d'un dossier.
     *
     * <p>Un document dont l'extraction échoue ne produit JAMAIS de
     * {@link DocumentAnalysis} : le compteur {@code done} (analyses en DONE)
     * plafonne sous {@code totalItems} (= TOUS les documents) et le job reste
     * {@code PROCESSING} à vie (incident prod RENVERSEZ 2026-05-19, dossier
     * stanojevic, OCR_UNSUPPORTED_SIZE). La complétion compte donc désormais aussi
     * les extractions FAILED : {@code terminal = done + failedExtractions}.
     *
     * <p>Gardes : le job passe DONE seulement s'il n'est pas déjà terminal
     * (idempotence — appelé par {@code finalizeAnalysis} ET par le listener
     * {@link #onExtractionFailed}), si {@code totalItems > 0}, si {@code terminal}
     * couvre {@code totalItems} et si au moins 1 document est exploitable
     * ({@code done > 0}) — un dossier sans aucune analyse réussie ne doit jamais
     * passer DONE.
     */
    private void recomputeDocumentAnalysisJobCompletion(UUID caseFileId) {
        if (caseFileId == null) return;

        analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS).ifPresent(job -> {
            long done = documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(
                    caseFileId, AnalysisStatus.DONE);
            // SF-121-05 : les extractions FAILED ne produisent pas de DocumentAnalysis ;
            // elles comptent comme items "terminaux" pour ne pas bloquer le job.
            long failedExtractions = extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(
                    caseFileId, ExtractionStatus.FAILED);
            long terminal = done + failedExtractions;

            // clamp to totalItems to prevent progressPercentage > 100 under race conditions
            int processedItems = (int) Math.min(terminal, job.getTotalItems());
            job.setProcessedItems(processedItems);

            // SF-121-05 : garde de statut → ne jamais repasser un job déjà terminal,
            // garde totalItems > 0, et garde done > 0 (au moins 1 doc exploitable —
            // un dossier 0 analyse réussie ne passe jamais DONE).
            boolean notTerminal = job.getStatus() != AnalysisStatus.DONE
                    && job.getStatus() != AnalysisStatus.FAILED;
            if (notTerminal && job.getTotalItems() > 0 && terminal >= job.getTotalItems() && done > 0) {
                job.setStatus(AnalysisStatus.DONE);
            }
            analysisJobRepository.save(job);
        });
    }

    /**
     * SF-121-05 : ré-évalue le job DOCUMENT_ANALYSIS quand une extraction passe en
     * FAILED. Couvre le cas où le document en échec d'extraction est le DERNIER à
     * se résoudre : aucune {@code finalizeAnalysis} ne tourne après lui, donc sans
     * ce listener la complétion ne serait jamais recalculée et le job resterait
     * PROCESSING jusqu'au reset zombie (42 min).
     *
     * <p>{@code AFTER_COMMIT} : l'extraction FAILED est déjà committée, le comptage
     * est cohérent. Le listener s'exécute hors transaction → la ré-évaluation passe
     * par {@code self} (proxy Spring) pour bénéficier de {@code @Transactional},
     * comme le fait déjà {@link #consumeDocumentAnalysis}.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtractionFailed(ExtractionFailedEvent event) {
        UUID caseFileId = extractionRepository.findCaseFileIdById(event.extractionId())
                .orElseGet(() -> documentRepository.findById(event.documentId())
                        .map(d -> d.getCaseFile() != null ? d.getCaseFile().getId() : null)
                        .orElse(null));
        if (caseFileId == null) {
            log.warn("SF-121-05 : extraction FAILED {} — caseFile introuvable, ré-évaluation du job ignorée",
                    event.extractionId());
            return;
        }
        self.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);
    }

    /**
     * SF-121-05 : point d'entrée transactionnel appelé par {@link #onExtractionFailed}.
     * Réutilise exactement la logique de complétion de {@link #recomputeDocumentAnalysisJobCompletion} ;
     * la garde de statut la rend idempotente.
     */
    @Transactional
    public void reevaluateDocumentAnalysisJobAfterFailedExtraction(UUID caseFileId) {
        recomputeDocumentAnalysisJobCompletion(caseFileId);
    }

    private String buildAggregatedPrompt(List<ChunkAnalysis> chunkAnalyses) {
        return chunkAnalyses.stream()
                .sorted((a, b) -> a.getChunk().getChunkIndex() - b.getChunk().getChunkIndex())
                .map(ca -> "Chunk %d : %s".formatted(ca.getChunk().getChunkIndex(), ca.getAnalysisResult()))
                .collect(Collectors.joining("\n"));
    }
}
