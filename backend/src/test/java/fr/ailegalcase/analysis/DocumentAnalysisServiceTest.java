package fr.ailegalcase.analysis;

import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DocumentAnalysisServiceTest {

    private final ChunkAnalysisRepository chunkAnalysisRepository = mock(ChunkAnalysisRepository.class);
    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final DocumentExtractionRepository extractionRepository = mock(DocumentExtractionRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);

    private final DocumentAnalysisService service = new DocumentAnalysisService(
            chunkAnalysisRepository, documentAnalysisRepository, extractionRepository, anthropicService);

    // U-01 : analyses de chunks valides → DocumentAnalysis DONE
    @Test
    void consumeDocumentAnalysis_validChunkAnalyses_persistsDoneAnalysis() {
        UUID extractionId = UUID.randomUUID();

        DocumentChunk chunk0 = chunkWithIndex(0);
        DocumentChunk chunk1 = chunkWithIndex(1);

        ChunkAnalysis ca0 = chunkAnalysis(chunk0, "{\"faits\":[\"fait1\"]}");
        ChunkAnalysis ca1 = chunkAnalysis(chunk1, "{\"faits\":[\"fait2\"]}");

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(new Document());

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca0, ca1));
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{\"faits\":[\"synthese\"]}", "claude-sonnet-4-6", 200, 100));

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<DocumentAnalysis> captor = ArgumentCaptor.forClass(DocumentAnalysis.class);
        verify(documentAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(captor.getValue().getAnalysisResult()).isEqualTo("{\"faits\":[\"synthese\"]}");
    }

    // U-02 : erreur Anthropic → DocumentAnalysis FAILED
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

        DocumentChunk chunk0 = chunkWithIndex(0);
        DocumentChunk chunk1 = chunkWithIndex(1);

        ChunkAnalysis ca1 = chunkAnalysis(chunk1, "{\"faits\":[\"B\"]}");
        ChunkAnalysis ca0 = chunkAnalysis(chunk0, "{\"faits\":[\"A\"]}");

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(new Document());

        when(chunkAnalysisRepository.findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(List.of(ca1, ca0)); // ordre inversé intentionnel
        when(extractionRepository.findById(extractionId)).thenReturn(Optional.of(extraction));
        when(documentAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));

        service.consumeDocumentAnalysis(new DocumentAnalysisMessage(extractionId));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeChunk(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt.indexOf("Chunk 0")).isLessThan(prompt.indexOf("Chunk 1"));
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
