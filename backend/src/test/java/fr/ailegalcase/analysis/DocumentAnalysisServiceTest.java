package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

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

    private final DocumentAnalysisService service = new DocumentAnalysisService(
            chunkAnalysisRepository, documentAnalysisRepository, extractionRepository,
            documentRepository, anthropicService, analysisJobRepository, usageEventService, caseFileRepository);

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
        when(anthropicService.analyzeFast(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[\"synthese\"]}", "claude-haiku-4-5-20251001", 200, 100));
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<DocumentAnalysis> captor = ArgumentCaptor.forClass(DocumentAnalysis.class);
        verify(documentAnalysisRepository, times(3)).save(captor.capture());
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

    // U-02 : erreur Anthropic → DocumentAnalysis FAILED (pas de trigger)
    @Test
    void consumeDocumentAnalysis_anthropicError_persistsFailedAnalysis() {
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

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(extractionRepository.findCaseFileIdById(extractionId)).thenReturn(Optional.of(caseFileId));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.DOCUMENT_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeFast(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<DocumentAnalysis> captor = ArgumentCaptor.forClass(DocumentAnalysis.class);
        verify(documentAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
    }

    // U-03 : aucune chunk_analysis DONE → aucune DocumentAnalysis créée
    @Test
    void consumeDocumentAnalysis_noChunkAnalyses_noDocumentAnalysisCreated() {
        UUID extractionId = UUID.randomUUID();
        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of());

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

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
        when(anthropicService.analyzeFast(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-haiku-4-5-20251001", 10, 5));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeFast(any(), promptCaptor.capture(), anyInt());
        String prompt = promptCaptor.getValue();
        assertThat(prompt.indexOf("Chunk 0")).isLessThan(prompt.indexOf("Chunk 1"));
    }

    // U-05 : le SYSTEM_PROMPT contient les contraintes de longueur
    @Test
    void systemPrompt_containsLengthConstraints() {
        assertThat(DocumentAnalysisService.SYSTEM_PROMPT).contains("5 faits maximum");
        assertThat(DocumentAnalysisService.SYSTEM_PROMPT).contains("3 points_juridiques maximum");
        assertThat(DocumentAnalysisService.SYSTEM_PROMPT).contains("3 risques maximum");
        assertThat(DocumentAnalysisService.SYSTEM_PROMPT).contains("3 questions_ouvertes maximum");
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
