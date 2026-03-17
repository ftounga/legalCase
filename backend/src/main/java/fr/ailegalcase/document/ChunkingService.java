package fr.ailegalcase.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChunkingService {

    private static final Logger log = LoggerFactory.getLogger(ChunkingService.class);

    static final int CHUNK_SIZE = 1000;
    static final int OVERLAP = 200;

    private final DocumentExtractionRepository extractionRepository;
    private final DocumentChunkRepository chunkRepository;

    public ChunkingService(DocumentExtractionRepository extractionRepository,
                           DocumentChunkRepository chunkRepository) {
        this.extractionRepository = extractionRepository;
        this.chunkRepository = chunkRepository;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtractionDone(ExtractionDoneEvent event) {
        chunk(event.extractionId(), event.extractedText());
    }

    @Transactional
    public void chunk(UUID extractionId, String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            log.warn("Extraction {} has empty text — skipping chunking", extractionId);
            return;
        }

        DocumentExtraction extractionRef = extractionRepository.getReferenceById(extractionId);
        List<DocumentChunk> chunks = buildChunks(extractedText, extractionRef);

        chunkRepository.saveAll(chunks);
        log.info("Chunking done for extraction {} — {} chunks created", extractionId, chunks.size());
    }

    private List<DocumentChunk> buildChunks(String text, DocumentExtraction extraction) {
        List<DocumentChunk> chunks = new ArrayList<>();
        int start = 0;
        int index = 0;

        while (start < text.length()) {
            int end = Math.min(start + CHUNK_SIZE, text.length());
            String chunkText = text.substring(start, end);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setExtraction(extraction);
            chunk.setChunkIndex(index);
            chunk.setChunkText(chunkText);
            chunk.setTokenCount(chunkText.length() / 4);
            chunk.setChunkMetadata("{\"startChar\":%d,\"endChar\":%d}".formatted(start, end));
            chunks.add(chunk);

            if (end == text.length()) break;

            index++;
            int nextStart = end - OVERLAP;
            if (nextStart <= start) break;
            start = nextStart;
        }

        return chunks;
    }
}
