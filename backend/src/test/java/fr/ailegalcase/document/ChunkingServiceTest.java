package fr.ailegalcase.document;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class ChunkingServiceTest {

    private final DocumentExtractionRepository extractionRepository =
            mock(DocumentExtractionRepository.class);
    private final DocumentChunkRepository chunkRepository =
            mock(DocumentChunkRepository.class);

    private final ChunkingService service = new ChunkingService(extractionRepository, chunkRepository);

    // U-01 : texte < 1000 chars → 1 chunk
    @Test
    void chunk_shortText_produces1Chunk() {
        UUID extractionId = UUID.randomUUID();
        DocumentExtraction extraction = new DocumentExtraction();
        when(extractionRepository.getReferenceById(extractionId)).thenReturn(extraction);
        when(chunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        service.chunk(extractionId, "Texte court.");

        verify(chunkRepository).saveAll(argThat(list -> ((java.util.List<?>) list).size() == 1));
    }

    // U-02 : texte de 2500 chars → 3 chunks avec overlap
    @Test
    void chunk_longText_producesMultipleChunksWithOverlap() {
        UUID extractionId = UUID.randomUUID();
        DocumentExtraction extraction = new DocumentExtraction();
        when(extractionRepository.getReferenceById(extractionId)).thenReturn(extraction);
        when(chunkRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        String text = "a".repeat(2500);
        service.chunk(extractionId, text);

        // chunk 0: 0-1000, chunk 1: 800-1800, chunk 2: 1600-2500
        verify(chunkRepository).saveAll(argThat(list -> ((java.util.List<?>) list).size() == 3));
    }

    // U-03 : texte vide → 0 chunks
    @Test
    void chunk_emptyText_producesNoChunks() {
        UUID extractionId = UUID.randomUUID();

        service.chunk(extractionId, "");

        verifyNoInteractions(extractionRepository, chunkRepository);
    }

    // U-04 : texte null → 0 chunks
    @Test
    void chunk_nullText_producesNoChunks() {
        UUID extractionId = UUID.randomUUID();

        service.chunk(extractionId, null);

        verifyNoInteractions(extractionRepository, chunkRepository);
    }

    // U-05 : tokenCount = longueur / 4
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

        service.chunk(extractionId, "a".repeat(400));

        assertThat(saved).hasSize(1);
        assertThat(saved.get(0).getTokenCount()).isEqualTo(100); // 400 / 4
    }
}
