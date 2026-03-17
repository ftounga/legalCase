package fr.ailegalcase.analysis;

import fr.ailegalcase.document.ChunkingDoneEvent;
import fr.ailegalcase.document.DocumentChunk;
import fr.ailegalcase.document.DocumentChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Service
@Profile("local")
public class ChunkAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ChunkAnalysisService.class);

    private final RabbitTemplate rabbitTemplate;
    private final DocumentChunkRepository chunkRepository;
    private final ChunkAnalysisRepository analysisRepository;
    private final AnthropicService anthropicService;

    public ChunkAnalysisService(RabbitTemplate rabbitTemplate,
                                DocumentChunkRepository chunkRepository,
                                ChunkAnalysisRepository analysisRepository,
                                AnthropicService anthropicService) {
        this.rabbitTemplate = rabbitTemplate;
        this.chunkRepository = chunkRepository;
        this.analysisRepository = analysisRepository;
        this.anthropicService = anthropicService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChunkingDone(ChunkingDoneEvent event) {
        log.debug("Publishing {} chunk analysis messages for extraction {}",
                event.chunkIds().size(), event.extractionId());
        for (UUID chunkId : event.chunkIds()) {
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CHUNK_ANALYSIS_EXCHANGE,
                    RabbitMQConfig.CHUNK_ANALYSIS_ROUTING_KEY,
                    new ChunkAnalysisMessage(chunkId)
            );
        }
    }

    @RabbitListener(queues = RabbitMQConfig.CHUNK_ANALYSIS_QUEUE)
    public void consumeChunkAnalysis(ChunkAnalysisMessage message) {
        UUID chunkId = message.chunkId();

        DocumentChunk chunk = chunkRepository.findById(chunkId).orElse(null);
        if (chunk == null) {
            log.error("Chunk {} not found — analysis skipped", chunkId);
            return;
        }

        if (chunk.getChunkText() == null || chunk.getChunkText().isBlank()) {
            log.warn("Chunk {} has empty text — analysis skipped", chunkId);
            return;
        }

        ChunkAnalysis analysis = new ChunkAnalysis();
        analysis.setChunk(chunk);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = analysisRepository.save(analysis);

        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = analysisRepository.save(analysis);

        try {
            AnthropicResult result = anthropicService.analyzeChunk(chunk.getChunkText());
            analysis.setAnalysisResult(result.content());
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            log.info("Analysis DONE for chunk {}", chunkId);
        } catch (Exception e) {
            log.error("Analysis FAILED for chunk {}", chunkId, e);
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }

        analysisRepository.save(analysis);
    }
}
