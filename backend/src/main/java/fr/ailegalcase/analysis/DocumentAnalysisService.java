package fr.ailegalcase.analysis;

import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Profile("local")
public class DocumentAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(DocumentAnalysisService.class);

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert en droit du travail français.
            Tu reçois les analyses de plusieurs segments d'un document juridique.
            Produis une synthèse globale du document en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            """;

    private final ChunkAnalysisRepository chunkAnalysisRepository;
    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final DocumentExtractionRepository extractionRepository;
    private final AnthropicService anthropicService;

    public DocumentAnalysisService(ChunkAnalysisRepository chunkAnalysisRepository,
                                   DocumentAnalysisRepository documentAnalysisRepository,
                                   DocumentExtractionRepository extractionRepository,
                                   AnthropicService anthropicService) {
        this.chunkAnalysisRepository = chunkAnalysisRepository;
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.extractionRepository = extractionRepository;
        this.anthropicService = anthropicService;
    }

    @RabbitListener(queues = RabbitMQConfig.DOCUMENT_ANALYSIS_QUEUE)
    public void consumeDocumentAnalysis(DocumentAnalysisMessage message) {
        UUID extractionId = message.extractionId();

        List<ChunkAnalysis> chunkAnalyses = chunkAnalysisRepository
                .findByChunkExtractionIdAndAnalysisStatus(extractionId, AnalysisStatus.DONE);

        if (chunkAnalyses.isEmpty()) {
            log.warn("No DONE chunk analyses found for extraction {} — document analysis skipped", extractionId);
            return;
        }

        DocumentExtraction extraction = extractionRepository.findById(extractionId).orElse(null);
        if (extraction == null) {
            log.error("Extraction {} not found — document analysis skipped", extractionId);
            return;
        }

        DocumentAnalysis analysis = new DocumentAnalysis();
        analysis.setDocument(extraction.getDocument());
        analysis.setExtraction(extraction);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = documentAnalysisRepository.save(analysis);

        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = documentAnalysisRepository.save(analysis);

        try {
            String aggregatedPrompt = buildAggregatedPrompt(chunkAnalyses);
            AnthropicResult result = anthropicService.analyzeChunk(aggregatedPrompt);
            analysis.setAnalysisResult(result.content());
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            log.info("Document analysis DONE for extraction {}", extractionId);
        } catch (Exception e) {
            log.error("Document analysis FAILED for extraction {}", extractionId, e);
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }

        documentAnalysisRepository.save(analysis);
    }

    private String buildAggregatedPrompt(List<ChunkAnalysis> chunkAnalyses) {
        return chunkAnalyses.stream()
                .sorted((a, b) -> a.getChunk().getChunkIndex() - b.getChunk().getChunkIndex())
                .map(ca -> "Chunk %d : %s".formatted(ca.getChunk().getChunkIndex(), ca.getAnalysisResult()))
                .collect(Collectors.joining("\n"));
    }
}
