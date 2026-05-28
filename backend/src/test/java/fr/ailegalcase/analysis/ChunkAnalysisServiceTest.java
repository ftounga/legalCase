package fr.ailegalcase.analysis;

import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.ChunkingDoneEvent;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChunkAnalysisServiceTest {

    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final DocumentChunkRepository chunkRepository = mock(DocumentChunkRepository.class);
    private final ChunkAnalysisRepository analysisRepository = mock(ChunkAnalysisRepository.class);
    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final DocumentExtractionRepository extractionRepository = mock(DocumentExtractionRepository.class);
    private final UsageEventService usageEventService = mock(UsageEventService.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final PlanLimitService planLimitService = mock(PlanLimitService.class);

    private final ChunkAnalysisService service = new ChunkAnalysisService(
            rabbitTemplate, chunkRepository, analysisRepository, documentAnalysisRepository,
            anthropicService, analysisJobRepository, extractionRepository,
            usageEventService, caseFileRepository, planLimitService);

    // U-01 : onChunkingDone publie 1 message par chunk + crée le job
    @Test
    void onChunkingDone_publishesOneMessagePerChunkAndCreatesJob() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(caseFileRepository.findContextById(caseFileId)).thenReturn(Optional.empty());
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onChunkingDone(new ChunkingDoneEvent(extractionId, chunkIds));

        verify(rabbitTemplate, times(3)).convertAndSend(
                eq(RabbitMQConfig.CHUNK_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CHUNK_ANALYSIS_ROUTING_KEY),
                any(ChunkAnalysisMessage.class)
        );

        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getTotalItems()).isEqualTo(3);
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.PENDING);
        assertThat(jobCaptor.getValue().getJobType()).isEqualTo(JobType.CHUNK_ANALYSIS);
    }

    // U-02 : consumer texte valide → analyse DONE + job mis à jour
    @Test
    void consumeChunkAnalysis_validChunk_persistsDoneAnalysisAndUpdatesJob() {
        UUID chunkId = UUID.randomUUID();
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(extractionId);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique valide.");
        chunk.setExtraction(extraction);

        AnalysisJob job = new AnalysisJob();
        job.setCaseFileId(caseFileId);
        job.setJobType(JobType.CHUNK_ANALYSIS);
        job.setStatus(AnalysisStatus.PENDING);
        job.setTotalItems(1);
        job.setProcessedItems(0);

        UUID userId = UUID.randomUUID();

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any(AiCallContext.class), any(), any(), any())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));
        when(chunkRepository.countByExtractionId(extractionId)).thenReturn(1L);
        when(analysisRepository.countByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(documentAnalysisRepository.existsByExtractionIdAndAnalysisStatusIn(eq(extractionId), any()))
                .thenReturn(false);
        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisRepository.countByChunkExtractionDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeChunkAnalysis(ChunkAnalysisMessage.forChunk(chunkId));

        verify(anthropicService).analyzeChunk(any(AiCallContext.class), eq("Texte juridique valide."), any(), any());

        ArgumentCaptor<ChunkAnalysis> captor = ArgumentCaptor.forClass(ChunkAnalysis.class);
        verify(analysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(captor.getValue().getAnalysisResult()).isEqualTo("{\"faits\":[]}");
        assertThat(captor.getValue().getModelUsed()).isEqualTo("claude-sonnet-4-6");
        assertThat(captor.getValue().getPromptTokens()).isEqualTo(100);
        assertThat(captor.getValue().getCompletionTokens()).isEqualTo(50);

        // Tous les chunks DONE → DocumentAnalysisMessage publié
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_ROUTING_KEY),
                any(DocumentAnalysisMessage.class)
        );

        // Job mis à jour : processedItems=1, status=DONE
        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getProcessedItems()).isEqualTo(1);
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.DONE);

        // Usage enregistré
        verify(usageEventService).record(caseFileId, userId, JobType.CHUNK_ANALYSIS, 100, 50);
    }

    // U-03 : erreur Anthropic → analyse FAILED (pas de job update ni trigger)
    @Test
    void consumeChunkAnalysis_anthropicError_persistsFailedAnalysis() {
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique.");

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any(AiCallContext.class), any(), any(), any())).thenThrow(new RuntimeException("API error"));

        service.consumeChunkAnalysis(ChunkAnalysisMessage.forChunk(chunkId));

        ArgumentCaptor<ChunkAnalysis> captor = ArgumentCaptor.forClass(ChunkAnalysis.class);
        verify(analysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        verifyNoInteractions(analysisJobRepository);
    }

    // U-04 : chunk introuvable → aucune analyse créée
    @Test
    void consumeChunkAnalysis_chunkNotFound_noAnalysisCreated() {
        UUID chunkId = UUID.randomUUID();
        when(chunkRepository.findById(chunkId)).thenReturn(Optional.empty());

        service.consumeChunkAnalysis(ChunkAnalysisMessage.forChunk(chunkId));

        verifyNoInteractions(analysisRepository, anthropicService);
    }

    // U-05 : chunk texte vide → aucune analyse créée
    @Test
    void consumeChunkAnalysis_emptyChunkText_noAnalysisCreated() {
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("");

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));

        service.consumeChunkAnalysis(ChunkAnalysisMessage.forChunk(chunkId));

        verifyNoInteractions(analysisRepository, anthropicService);
    }

    // U-06 : tous les chunks pas encore DONE → aucun DocumentAnalysisMessage publié
    @Test
    void triggerDocumentAnalysis_notAllChunksDone_noDocumentAnalysisMessagePublished() {
        UUID chunkId = UUID.randomUUID();
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(extractionId);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique.");
        chunk.setExtraction(extraction);

        AnalysisJob job = new AnalysisJob();
        job.setTotalItems(3);
        job.setProcessedItems(0);

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any(AiCallContext.class), any(), any(), any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(chunkRepository.countByExtractionId(extractionId)).thenReturn(3L);
        when(analysisRepository.countByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(1L); // 1 DONE sur 3 → pas encore prêt
        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisRepository.countByChunkExtractionDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeChunkAnalysis(ChunkAnalysisMessage.forChunk(chunkId));

        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_ROUTING_KEY),
                any(DocumentAnalysisMessage.class)
        );
    }

    // U-07 : DocumentAnalysis déjà existante → aucun DocumentAnalysisMessage publié (idempotence)
    @Test
    void triggerDocumentAnalysis_alreadyExists_noDocumentAnalysisMessagePublished() {
        UUID chunkId = UUID.randomUUID();
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(extractionId);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique.");
        chunk.setExtraction(extraction);

        AnalysisJob job = new AnalysisJob();
        job.setTotalItems(1);
        job.setProcessedItems(0);

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any(AiCallContext.class), any(), any(), any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(chunkRepository.countByExtractionId(extractionId)).thenReturn(1L);
        when(analysisRepository.countByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(documentAnalysisRepository.existsByExtractionIdAndAnalysisStatusIn(eq(extractionId), any()))
                .thenReturn(true); // déjà déclenchée
        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisRepository.countByChunkExtractionDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeChunkAnalysis(ChunkAnalysisMessage.forChunk(chunkId));

        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_ROUTING_KEY),
                any(DocumentAnalysisMessage.class)
        );
    }

    // U-08 : onChunkingDone cumule totalItems si plusieurs extractions pour la même case
    @Test
    void onChunkingDone_existingJob_accumulatesTotalItems() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        AnalysisJob existingJob = new AnalysisJob();
        existingJob.setCaseFileId(caseFileId);
        existingJob.setJobType(JobType.CHUNK_ANALYSIS);
        existingJob.setStatus(AnalysisStatus.PENDING);
        existingJob.setTotalItems(3); // déjà 3 chunks d'une extraction précédente
        existingJob.setProcessedItems(0);

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(caseFileRepository.findContextById(caseFileId)).thenReturn(Optional.empty());
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.of(existingJob));
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onChunkingDone(new ChunkingDoneEvent(extractionId, chunkIds));

        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getTotalItems()).isEqualTo(5); // 3 + 2
    }

    // U-09 : message enrichi → pas d'appel DB findCaseFileIdById ni findWorkspaceIdById, analyse DONE avec bon contexte
    @Test
    void consumeChunkAnalysis_enrichedMessage_noCaseFileLookups() {
        UUID chunkId = UUID.randomUUID();
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(extractionId);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Contrat de travail clause 1.");
        chunk.setExtraction(extraction);

        AnalysisJob job = new AnalysisJob();
        job.setCaseFileId(caseFileId);
        job.setTotalItems(1);
        job.setProcessedItems(0);

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any(AiCallContext.class), any(), eq("DROIT_DU_TRAVAIL"), eq("FRANCE"))).thenReturn(
                new AnthropicResult("{\"faits\":[\"fait\"]}", "claude-haiku-4-5-20251001", 80, 40));
        when(chunkRepository.countByExtractionId(extractionId)).thenReturn(1L);
        when(analysisRepository.countByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(documentAnalysisRepository.existsByExtractionIdAndAnalysisStatusIn(eq(extractionId), any()))
                .thenReturn(false);
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.of(job));
        when(analysisRepository.countByChunkExtractionDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ChunkAnalysisMessage enrichedMsg = new ChunkAnalysisMessage(
                chunkId, caseFileId, workspaceId, "DROIT_DU_TRAVAIL", "FRANCE", userId);

        service.consumeChunkAnalysis(enrichedMsg);

        // DB lookups pour caseFileId et workspaceId ne doivent PAS être appelés
        verify(extractionRepository, never()).findCaseFileIdById(any());
        verify(caseFileRepository, never()).findWorkspaceIdById(any());
        verify(caseFileRepository, never()).findLegalDomainById(any());
        verify(caseFileRepository, never()).findCountryById(any());
        verify(caseFileRepository, never()).findCreatedByUserIdById(any());

        // Analyse DONE avec le bon legalDomain
        verify(anthropicService).analyzeChunk(any(AiCallContext.class), any(), eq("DROIT_DU_TRAVAIL"), eq("FRANCE"));

        // Usage enregistré directement via userId du message
        verify(usageEventService).record(caseFileId, userId, JobType.CHUNK_ANALYSIS, 80, 40);
    }

    // U-10 : onChunkingDone avec contexte disponible → messages enrichis publiés
    @Test
    void onChunkingDone_withContext_publishesEnrichedMessages() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID());

        CaseFileContext context = new CaseFileContext(workspaceId, "DROIT_IMMIGRATION", "FRANCE", userId);

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(caseFileRepository.findContextById(caseFileId)).thenReturn(Optional.of(context));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onChunkingDone(new ChunkingDoneEvent(extractionId, chunkIds));

        ArgumentCaptor<ChunkAnalysisMessage> msgCaptor = ArgumentCaptor.forClass(ChunkAnalysisMessage.class);
        verify(rabbitTemplate, times(2)).convertAndSend(
                eq(RabbitMQConfig.CHUNK_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CHUNK_ANALYSIS_ROUTING_KEY),
                msgCaptor.capture()
        );
        ChunkAnalysisMessage published = msgCaptor.getValue();
        assertThat(published.caseFileId()).isEqualTo(caseFileId);
        assertThat(published.workspaceId()).isEqualTo(workspaceId);
        assertThat(published.legalDomain()).isEqualTo("DROIT_IMMIGRATION");
        assertThat(published.country()).isEqualTo("FRANCE");
        assertThat(published.userId()).isEqualTo(userId);
    }

    // U-11 : onChunkingDone quand findContextById retourne empty → messages sans contexte (pas d'erreur)
    @Test
    void onChunkingDone_contextNotFound_publishesMessagesWithoutContext() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();
        List<UUID> chunkIds = List.of(UUID.randomUUID());

        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(caseFileRepository.findContextById(caseFileId)).thenReturn(Optional.empty());
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CHUNK_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.onChunkingDone(new ChunkingDoneEvent(extractionId, chunkIds));

        ArgumentCaptor<ChunkAnalysisMessage> msgCaptor = ArgumentCaptor.forClass(ChunkAnalysisMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CHUNK_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CHUNK_ANALYSIS_ROUTING_KEY),
                msgCaptor.capture()
        );
        ChunkAnalysisMessage published = msgCaptor.getValue();
        assertThat(published.caseFileId()).isNull();
        assertThat(published.workspaceId()).isNull();
        assertThat(published.legalDomain()).isNull();
        assertThat(published.country()).isNull();
        assertThat(published.userId()).isNull();
    }
}
