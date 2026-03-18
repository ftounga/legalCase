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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DocumentAnalysisServiceTest {

    private final ChunkAnalysisRepository chunkAnalysisRepository = mock(ChunkAnalysisRepository.class);
    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final DocumentExtractionRepository extractionRepository = mock(DocumentExtractionRepository.class);
    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);

    private final DocumentAnalysisService service = new DocumentAnalysisService(
            chunkAnalysisRepository, documentAnalysisRepository, extractionRepository,
            documentRepository, caseAnalysisRepository, rabbitTemplate, anthropicService);

    // U-01 : analyses de chunks valides → DocumentAnalysis DONE + trigger case analysis
    @Test
    void consumeDocumentAnalysis_validChunkAnalyses_persistsDoneAnalysis() {
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

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca0, ca1));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{\"faits\":[\"synthese\"]}", "claude-sonnet-4-6", 200, 100));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(caseFileId), any()))
                .thenReturn(false);

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<DocumentAnalysis> captor = ArgumentCaptor.forClass(DocumentAnalysis.class);
        verify(documentAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(captor.getValue().getAnalysisResult()).isEqualTo("{\"faits\":[\"synthese\"]}");

        // Tous les documents DONE → CaseAnalysisMessage publié
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CASE_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CASE_ANALYSIS_ROUTING_KEY),
                any(CaseAnalysisMessage.class)
        );
    }

    // U-02 : erreur Anthropic → DocumentAnalysis FAILED (pas de trigger)
    @Test
    void consumeDocumentAnalysis_anthropicError_persistsFailedAnalysis() {
        UUID extractionId = UUID.randomUUID();

        DocumentChunk chunk = chunkWithIndex(0);
        ChunkAnalysis ca = chunkAnalysis(chunk, "{\"faits\":[]}");

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(new Document());

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenThrow(new RuntimeException("API error"));

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<DocumentAnalysis> captor = ArgumentCaptor.forClass(DocumentAnalysis.class);
        verify(documentAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        verifyNoInteractions(rabbitTemplate);
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
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(caseFileId), any()))
                .thenReturn(false);

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeChunk(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt.indexOf("Chunk 0")).isLessThan(prompt.indexOf("Chunk 1"));
    }

    // U-05 : pas tous les documents DONE → aucun CaseAnalysisMessage publié
    @Test
    void triggerCaseAnalysis_notAllDocumentsDone_noCaseAnalysisMessagePublished() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);

        Document document = new Document();
        document.setCaseFile(caseFile);

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(document);

        ChunkAnalysis ca = chunkAnalysis(chunkWithIndex(0), "{\"faits\":[]}");

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(3L);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L); // 1 DONE sur 3

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.CASE_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CASE_ANALYSIS_ROUTING_KEY),
                any(CaseAnalysisMessage.class)
        );
    }

    // U-06 : CaseAnalysis déjà existante → aucun CaseAnalysisMessage publié (idempotence)
    @Test
    void triggerCaseAnalysis_alreadyExists_noCaseAnalysisMessagePublished() {
        UUID extractionId = UUID.randomUUID();
        UUID caseFileId = UUID.randomUUID();

        CaseFile caseFile = new CaseFile();
        caseFile.setId(caseFileId);

        Document document = new Document();
        document.setCaseFile(caseFile);

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(document);

        ChunkAnalysis ca = chunkAnalysis(chunkWithIndex(0), "{\"faits\":[]}");

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(documentRepository.countByCaseFileId(caseFileId)).thenReturn(1L);
        when(documentAnalysisRepository.countByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(caseAnalysisRepository.existsByCaseFileIdAndAnalysisStatusIn(eq(caseFileId), any()))
                .thenReturn(true); // déjà déclenchée

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.CASE_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CASE_ANALYSIS_ROUTING_KEY),
                any(CaseAnalysisMessage.class)
        );
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
