package fr.ailegalcase.analysis;

import fr.ailegalcase.document.ChunkingDoneEvent;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentChunkRepository;
import fr.ailegalcase.document.DocumentExtraction;
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

    private final ChunkAnalysisService service = new ChunkAnalysisService(
            rabbitTemplate, chunkRepository, analysisRepository, documentAnalysisRepository, anthropicService);

    // U-01 : onChunkingDone publie 1 message par chunk
    @Test
    void onChunkingDone_publishesOneMessagePerChunk() {
        UUID extractionId = UUID.randomUUID();
        List<UUID> chunkIds = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        service.onChunkingDone(new ChunkingDoneEvent(extractionId, chunkIds));

        verify(rabbitTemplate, times(3)).convertAndSend(
                eq(RabbitMQConfig.CHUNK_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.CHUNK_ANALYSIS_ROUTING_KEY),
                any(ChunkAnalysisMessage.class)
        );
    }

    // U-02 : consumer texte valide → analyse DONE
    @Test
    void consumeChunkAnalysis_validChunk_persistsDoneAnalysis() {
        UUID chunkId = UUID.randomUUID();
        UUID extractionId = UUID.randomUUID();

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(extractionId);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique valide.");
        chunk.setExtraction(extraction);

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));
        when(chunkRepository.countByExtractionId(extractionId)).thenReturn(1L);
        when(analysisRepository.countByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(documentAnalysisRepository.existsByExtractionIdAndAnalysisStatusIn(eq(extractionId), any()))
                .thenReturn(false);

        service.consumeChunkAnalysis(new ChunkAnalysisMessage(chunkId));

        verify(anthropicService).analyzeChunk("Texte juridique valide.");

        ArgumentCaptor<ChunkAnalysis> captor = ArgumentCaptor.forClass(ChunkAnalysis.class);
        verify(analysisRepository, times(3)).save(captor.capture());

        // Le dernier appel save() a le statut final DONE
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
    }

    // U-03 : erreur Anthropic → analyse FAILED
    @Test
    void consumeChunkAnalysis_anthropicError_persistsFailedAnalysis() {
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique.");

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenThrow(new RuntimeException("API error"));

        service.consumeChunkAnalysis(new ChunkAnalysisMessage(chunkId));

        ArgumentCaptor<ChunkAnalysis> captor = ArgumentCaptor.forClass(ChunkAnalysis.class);
        verify(analysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
    }

    // U-04 : chunk introuvable → aucune analyse créée
    @Test
    void consumeChunkAnalysis_chunkNotFound_noAnalysisCreated() {
        UUID chunkId = UUID.randomUUID();
        when(chunkRepository.findById(chunkId)).thenReturn(Optional.empty());

        service.consumeChunkAnalysis(new ChunkAnalysisMessage(chunkId));

        verifyNoInteractions(analysisRepository, anthropicService);
    }

    // U-05 : chunk texte vide → aucune analyse créée
    @Test
    void consumeChunkAnalysis_emptyChunkText_noAnalysisCreated() {
        UUID chunkId = UUID.randomUUID();
        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("");

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));

        service.consumeChunkAnalysis(new ChunkAnalysisMessage(chunkId));

        verifyNoInteractions(analysisRepository, anthropicService);
    }

    // U-06 : tous les chunks pas encore DONE → aucun DocumentAnalysisMessage publié
    @Test
    void triggerDocumentAnalysis_notAllChunksDone_noDocumentAnalysisMessagePublished() {
        UUID chunkId = UUID.randomUUID();
        UUID extractionId = UUID.randomUUID();

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(extractionId);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique.");
        chunk.setExtraction(extraction);

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(chunkRepository.countByExtractionId(extractionId)).thenReturn(3L);
        when(analysisRepository.countByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(1L); // 1 DONE sur 3 → pas encore prêt

        service.consumeChunkAnalysis(new ChunkAnalysisMessage(chunkId));

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

        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setId(extractionId);

        DocumentChunk chunk = new DocumentChunk();
        chunk.setChunkText("Texte juridique.");
        chunk.setExtraction(extraction);

        when(chunkRepository.findById(chunkId)).thenReturn(Optional.of(chunk));
        when(analysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeChunk(any())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(chunkRepository.countByExtractionId(extractionId)).thenReturn(1L);
        when(analysisRepository.countByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE))
                .thenReturn(1L);
        when(documentAnalysisRepository.existsByExtractionIdAndAnalysisStatusIn(eq(extractionId), any()))
                .thenReturn(true); // déjà déclenchée

        service.consumeChunkAnalysis(new ChunkAnalysisMessage(chunkId));

        verify(rabbitTemplate, never()).convertAndSend(
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_ROUTING_KEY),
                any(DocumentAnalysisMessage.class)
        );
    }
}
