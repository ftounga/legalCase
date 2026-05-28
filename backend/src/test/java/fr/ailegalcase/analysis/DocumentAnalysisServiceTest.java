package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.document.ExtractionFailedEvent;
import fr.ailegalcase.document.ExtractionFailureReason;
import fr.ailegalcase.document.ExtractionStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class DocumentAnalysisServiceTest {

    private final ChunkAnalysisRepository chunkAnalysisRepository = mock(ChunkAnalysisRepository.class);
    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final DocumentExtractionRepository extractionRepository = mock(DocumentExtractionRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final UsageEventService usageEventService = mock(UsageEventService.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AnalysisLimitsProperties analysisLimitsProperties = mock(AnalysisLimitsProperties.class);

    private final DocumentAnalysisService service = new DocumentAnalysisService(
            chunkAnalysisRepository, documentAnalysisRepository, extractionRepository,
            documentRepository, anthropicService, analysisJobRepository, usageEventService,
            caseFileRepository, eventPublisher, analysisLimitsProperties);

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(service, "self", service);
        AnalysisLimitsProperties.LevelLimits limits = new AnalysisLimitsProperties.LevelLimits();
        limits.setFaits(5); limits.setPointsJuridiques(3); limits.setRisques(3); limits.setQuestionsOuvertes(3);
        AnalysisLimitsProperties.DomainLimits domainLimits = mock(AnalysisLimitsProperties.DomainLimits.class);
        when(domainLimits.getDocument()).thenReturn(limits);
        when(analysisLimitsProperties.forDomain(any())).thenReturn(domainLimits);
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // U-01 : analyses de chunks valides → DocumentAnalysis DONE + job mis à jour + usage enregistré
    @Test
    void consumeDocumentAnalysis_validChunkAnalyses_persistsDoneAnalysisAndCreatesJob() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);

        Document document = new Document();
        document.setCaseFile(caseFile);

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(document);

        DocumentChunk chunk0 = chunkWithIndex(0);
        DocumentChunk chunk1 = chunkWithIndex(1);
        ChunkAnalysis ca0 = chunkAnalysis(chunk0, "{\"faits\":[\"fait1\"]}");
        ChunkAnalysis ca1 = chunkAnalysis(chunk1, "{\"faits\":[\"fait2\"]}");

        AnalysisJob job = new AnalysisJob();
        job.setTotalItems(1);
        job.setProcessedItems(0);

        UUID userId = UUID.randomUUID();

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca0, ca1));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.findById(any())).thenAnswer(inv -> {
            DocumentAnalysis a = new DocumentAnalysis();
            a.setAnalysisStatus(AnalysisStatus.PROCESSING);
            return Optional.of(a);
        });
        when(anthropicService.analyzeFast(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[\"synthese\"]}", "claude-haiku-4-5-20251001", 200, 100));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId, false));

        ArgumentCaptor<DocumentAnalysis> captor = ArgumentCaptor.forClass(DocumentAnalysis.class);
        verify(documentAnalysisRepository, atLeast(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(captor.getValue().getAnalysisResult()).isEqualTo("{\"faits\":[\"synthese\"]}");

        // Job DOCUMENT_ANALYSIS mis à jour : processedItems=1, status=DONE
        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, atLeastOnce()).save(jobCaptor.capture());
        List<AnalysisJob> savedJobs = jobCaptor.getAllValues();
        AnalysisJob finalJob = savedJobs.get(savedJobs.size() - 1);
        assertThat(finalJob.getProcessedItems()).isEqualTo(1);
        assertThat(finalJob.getStatus()).isEqualTo(AnalysisStatus.DONE);

        // Usage enregistré
        verify(usageEventService).record(caseFileId, userId, JobType.DOCUMENT_ANALYSIS, 200, 100);
    }

    // U-02 : erreur Anthropic → DocumentAnalysis FAILED + job FAILED (F-147-01)
    @Test
    void consumeDocumentAnalysis_anthropicError_persistsFailedAnalysisAndMarksJobFailed() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        DocumentChunk chunk = chunkWithIndex(0);
        ChunkAnalysis ca = chunkAnalysis(chunk, "{\"faits\":[]}");

        Document document = new Document();
        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        document.setCaseFile(caseFile);

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(document);

        // F-147-01 : le job doit exister pour que markDocumentAnalysisJobFailed le trouve
        AnalysisJob existingJob = new AnalysisJob();
        existingJob.setCaseFileId(caseFileId);
        existingJob.setJobType(JobType.DOCUMENT_ANALYSIS);
        existingJob.setStatus(AnalysisStatus.PROCESSING);
        existingJob.setTotalItems(1);

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(existingJob));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.findById(any())).thenAnswer(inv -> {
            DocumentAnalysis a = new DocumentAnalysis();
            a.setAnalysisStatus(AnalysisStatus.PROCESSING);
            return Optional.of(a);
        });
        when(anthropicService.analyzeFast(any(AiCallContext.class), any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId, false));

        ArgumentCaptor<DocumentAnalysis> analysisCaptor = ArgumentCaptor.forClass(DocumentAnalysis.class);
        verify(documentAnalysisRepository, atLeast(3)).save(analysisCaptor.capture());
        assertThat(analysisCaptor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);

        // F-147-01 : le job DOCUMENT_ANALYSIS doit aussi être FAILED
        assertThat(existingJob.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(existingJob.getErrorMessage()).isNotBlank();
    }

    // U-03 : aucune chunk_analysis DONE → aucune DocumentAnalysis créée
    @Test
    void consumeDocumentAnalysis_noChunkAnalyses_noDocumentAnalysisCreated() {
        UUID extractionId = UUID.randomUUID();
        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of());

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId, false));

        verifyNoInteractions(documentAnalysisRepository, anthropicService, extractionRepository);
    }

    // U-04 : le prompt agrège les chunks dans l'ordre des index
    @Test
    void consumeDocumentAnalysis_promptContainsChunksInOrder() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);

        Document document = new Document();
        document.setCaseFile(caseFile);

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(document);

        DocumentChunk chunk0 = chunkWithIndex(0);
        DocumentChunk chunk1 = chunkWithIndex(1);
        ChunkAnalysis ca1 = chunkAnalysis(chunk1, "{\"faits\":[\"B\"]}");
        ChunkAnalysis ca0 = chunkAnalysis(chunk0, "{\"faits\":[\"A\"]}");

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca1, ca0)); // ordre inversé intentionnel
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.findById(any())).thenAnswer(inv -> {
            DocumentAnalysis a = new DocumentAnalysis();
            a.setAnalysisStatus(AnalysisStatus.PROCESSING);
            return Optional.of(a);
        });
        when(anthropicService.analyzeFast(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-haiku-4-5-20251001", 10, 5));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId, false));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeFast(any(AiCallContext.class), any(), promptCaptor.capture(), anyInt());
        String prompt = promptCaptor.getValue();
        assertThat(prompt.indexOf("Chunk 0")).isLessThan(prompt.indexOf("Chunk 1"));
    }

    // U-05 : le system prompt contient les contraintes de longueur
    @Test
    void systemPrompt_containsLengthConstraints() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(5); l.setPointsJuridiques(3); l.setRisques(3); l.setQuestionsOuvertes(3);
        String prompt = DocumentAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).contains("5 faits maximum");
        assertThat(prompt).contains("3 points_juridiques maximum");
        assertThat(prompt).contains("3 risques maximum");
        assertThat(prompt).contains("3 questions_ouvertes maximum");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SF-121-05 : job DOCUMENT_ANALYSIS terminal sur échec d'extraction partiel
    // ─────────────────────────────────────────────────────────────────────────

    // U-121-05-01 : done = totalItems, aucune extraction FAILED → DONE (non-régression)
    @Test
    void recompute_doneEqualsTotalNoFailure_jobDone() {
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(3);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(3L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(0L);

        service.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(job.getProcessedItems()).isEqualTo(3);
    }

    // U-121-05-02 : done + failedExtractions = totalItems, done >= 1 → DONE
    @Test
    void recompute_doneePlusFailedEqualsTotal_jobDone() {
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(3);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(2L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(1L);

        service.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(job.getProcessedItems()).isEqualTo(3);
    }

    // U-121-05-03 : done + failedExtractions < totalItems → reste PROCESSING
    @Test
    void recompute_terminalBelowTotal_jobStaysProcessing() {
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(6);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(3L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(1L);

        service.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.PROCESSING);
        assertThat(job.getProcessedItems()).isEqualTo(4);
    }

    // U-121-05-04 : done = 0, failedExtractions = totalItems → PAS DONE (0 doc exploitable)
    @Test
    void recompute_zeroDoneAllFailed_jobNotDone() {
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(3);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(0L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(3L);

        service.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.PROCESSING);
        assertThat(job.getProcessedItems()).isEqualTo(3);
    }

    // U-121-05-05 : done + failedExtractions > totalItems → processedItems clampé à totalItems
    @Test
    void recompute_terminalAboveTotal_processedItemsClamped() {
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(3);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(3L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(2L);

        service.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(job.getProcessedItems()).isEqualTo(3); // clampé, pas 5
    }

    // U-121-05-06 : job déjà FAILED → garde de statut, non repassé DONE
    @Test
    void recompute_jobAlreadyFailed_notReopenedToDone() {
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(3);
        job.setStatus(AnalysisStatus.FAILED);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(2L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(1L);

        service.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.FAILED);
    }

    // U-121-05-07 : job déjà DONE → garde de statut, reste DONE (idempotence)
    @Test
    void recompute_jobAlreadyDone_staysDone() {
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(3);
        job.setStatus(AnalysisStatus.DONE);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(3L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(0L);

        service.reevaluateDocumentAnalysisJobAfterFailedExtraction(caseFileId);

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    // U-121-05-08 : handler ExtractionFailedEvent → job PROCESSING + complétion atteinte → DONE
    @Test
    void onExtractionFailed_completionReached_jobDone() {
        UUID extractionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(3);

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(2L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(1L);

        service.onExtractionFailed(new ExtractionFailedEvent(
                extractionId, documentId, "scan.pdf", ExtractionFailureReason.OCR_UNSUPPORTED_SIZE));

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(job.getProcessedItems()).isEqualTo(3);
    }

    // U-121-05-09 : handler ExtractionFailedEvent — aucun job DOCUMENT_ANALYSIS → pas d'exception
    @Test
    void onExtractionFailed_noJob_noException() {
        UUID extractionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.empty());

        service.onExtractionFailed(new ExtractionFailedEvent(
                extractionId, documentId, "scan.pdf", ExtractionFailureReason.EMPTY_TEXT));

        verify(analysisJobRepository, never()).save(any());
    }

    // U-121-05-10 : handler ExtractionFailedEvent — caseFile irrésolvable → fallback documentRepository
    @Test
    void onExtractionFailed_caseFileResolvedViaDocument_jobRecomputed() {
        UUID extractionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        AnalysisJob job = jobProcessing(2);

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);
        Document document = new Document();
        document.setCaseFile(caseFile);

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.empty());
        when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(extractionRepository.countByDocumentCaseFileIdAndExtractionStatus(caseFileId, ExtractionStatus.FAILED))
                .thenReturn(1L);

        service.onExtractionFailed(new ExtractionFailedEvent(
                extractionId, documentId, "scan.pdf", ExtractionFailureReason.CORRUPTED));

        assertThat(job.getStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    // U-121-05-11 : handler ExtractionFailedEvent — caseFile totalement irrésolvable → pas d'exception
    @Test
    void onExtractionFailed_caseFileUnresolvable_noException() {
        UUID extractionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.empty());
        when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

        service.onExtractionFailed(new ExtractionFailedEvent(
                extractionId, documentId, "scan.pdf", ExtractionFailureReason.UNSUPPORTED_FORMAT));

        verifyNoInteractions(analysisJobRepository);
    }

    private AnalysisJob jobProcessing(int totalItems) {
        AnalysisJob job = new AnalysisJob();
        job.setJobType(JobType.DOCUMENT_ANALYSIS);
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(totalItems);
        job.setProcessedItems(0);
        return job;
    }

    // Helpers
    private DocumentChunk chunkWithIndex(int index) {
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkIndex(index);
        return chunk;
    }

    private ChunkAnalysis chunkAnalysis(DocumentChunk chunk, String result) {
        ChunkAnalysis ca = new ChunkAnalysis();
        ca.setChunk(chunk);
        ca.setAnalysisResult(result);
        ca.setAnalysisStatus(AnalysisStatus.DONE);
        return ca;
    }
}
