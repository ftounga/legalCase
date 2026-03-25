package fr.ailegalcase.document;

import fr.ailegalcase.analysis.DocumentAnalysisMessage;
import fr.ailegalcase.analysis.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.task.TaskExecutor;
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
    private final ApplicationEventPublisher eventPublisher;
    private final TaskExecutor taskExecutor;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.pipeline.direct-analysis-threshold-chars:600000}")
    private int directAnalysisThresholdChars;

    @Lazy @Autowired
    private ChunkingService self;

    public ChunkingService(DocumentExtractionRepository extractionRepository,
                           DocumentChunkRepository chunkRepository,
                           ApplicationEventPublisher eventPublisher,
                           TaskExecutor taskExecutor,
                           RabbitTemplate rabbitTemplate) {
        this.extractionRepository = extractionRepository;
        this.chunkRepository = chunkRepository;
        this.eventPublisher = eventPublisher;
        this.taskExecutor = taskExecutor;
        this.rabbitTemplate = rabbitTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onExtractionDone(ExtractionDoneEvent event) {
        taskExecutor.execute(() -> self.chunk(event.extractionId(), event.extractedText()));
    }

    @Transactional
    public void chunk(UUID extractionId, String extractedText) {
        if (extractedText == null || extractedText.isBlank()) {
            log.warn("Extraction {} has empty text — skipping chunking", extractionId);
            return;
        }

        if (extractedText.length() < directAnalysisThresholdChars) {
            log.info("Extraction {} — {} chars < threshold {} — direct document analysis",
                    extractionId, extractedText.length(), directAnalysisThresholdChars);
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.DOCUMENT_ANALYSIS_EXCHANGE,
                    RabbitMQConfig.DOCUMENT_ANALYSIS_ROUTING_KEY,
                    new DocumentAnalysisMessage(extractionId, true)
            );
            return;
        }

        DocumentExtraction extractionRef = extractionRepository.getReferenceById(extractionId);
        List<DocumentChunk> chunks = buildChunks(extractedText, extractionRef);

        List<DocumentChunk> saved = chunkRepository.saveAll(chunks);
        log.info("Chunking done for extraction {} — {} chunks created", extractionId, saved.size());

        List<UUID> chunkIds = saved.stream().map(DocumentChunk::getId).toList();
        eventPublisher.publishEvent(new ChunkingDoneEvent(extractionId, chunkIds));
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
