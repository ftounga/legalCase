package fr.ailegalcase.document;

import fr.ailegalcase.analysis.RabbitMQConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChunkingServiceTest {

    private final DocumentExtractionRepository extractionRepository =
            mock(DocumentExtractionRepository.class);
    private final DocumentChunkRepository chunkRepository =
            mock(DocumentChunkRepository.class);
    private final ApplicationEventPublisher eventPublisher =
            mock(ApplicationEventPublisher.class);
    private final RabbitTemplate rabbitTemplate =
            mock(RabbitTemplate.class);

    private ChunkingService service;

    @BeforeEach
    void setUp() {
        service = new ChunkingService(
                extractionRepository, chunkRepository, eventPublisher, Runnable::run, rabbitTemplate);
        ReflectionTestUtils.setField(service, "directAnalysisThresholdChars", 600000);
    }

    // U-01 : texte < seuil → direct analysis, 0 chunks
    @Test
    void chunk_shortText_sendsDirectAnalysis() {
        UUID extractionId = UUID.randomUUID();

        service.chunk(extractionId, "Texte court.");

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_EXCHANGE),
                eq(RabbitMQConfig.DOCUMENT_ANALYSIS_ROUTING_KEY),
                (Object) argThat(msg -> msg instanceof fr.ailegalcase.analysis.DocumentAnalysisMessage dam
                        && dam.directAnalysis()));
        verifyNoInteractions(chunkRepository);
    }

    // U-02 : texte >= seuil → chunking normal
    @Test
    void chunk_longText_producesChunks() {
        UUID extractionId = UUID.randomUUID();
        DocumentExtraction extraction = new DocumentExtraction();
        when(extractionRepository.getReferenceById(extractionId)).thenReturn(extraction);
        when(chunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        service.chunk(extractionId, "a".repeat(600001));

        verify(chunkRepository).saveAll(anyList());
        verifyNoInteractions(rabbitTemplate);
    }

    // U-03 : texte vide → rien
    @Test
    void chunk_emptyText_producesNothing() {
        service.chunk(UUID.randomUUID(), "");
        verifyNoInteractions(extractionRepository, chunkRepository, rabbitTemplate);
    }

    // U-04 : texte null → rien
    @Test
    void chunk_nullText_producesNothing() {
        service.chunk(UUID.randomUUID(), null);
        verifyNoInteractions(extractionRepository, chunkRepository, rabbitTemplate);
    }

    // U-05 : tokenCount = longueur / 4 (document long → chunking)
    @Test
    void chunk_tokenCountIsApproximated() {
        UUID extractionId = UUID.randomUUID();
        DocumentExtraction extraction = new DocumentExtraction();
        when(extractionRepository.getReferenceById(extractionId)).thenReturn(extraction);

        java.util.List<DocumentChunk> saved = new java.util.ArrayList<>();
        when(chunkRepository.saveAll(anyList())).thenAnswer(inv -> {
            saved.addAll(inv.getArgument(0));
            return saved;
        });

        service.chunk(extractionId, "a".repeat(600001));

        assertThat(saved).isNotEmpty();
        assertThat(saved.get(0).getTokenCount()).isEqualTo(250); // 1000 / 4
    }
}
