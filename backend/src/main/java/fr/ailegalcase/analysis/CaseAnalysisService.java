package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile("local")
public class CaseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisService.class);

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert en droit du travail français.
            Tu reçois les analyses de plusieurs documents d'un dossier juridique.
            Produis une synthèse globale du dossier en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            """;

    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AnthropicService anthropicService;

    public CaseAnalysisService(DocumentAnalysisRepository documentAnalysisRepository,
                               CaseAnalysisRepository caseAnalysisRepository,
                               CaseFileRepository caseFileRepository,
                               AnthropicService anthropicService) {
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.anthropicService = anthropicService;
    }

    @RabbitListener(queues = RabbitMQConfig.CASE_ANALYSIS_QUEUE)
    public void consumeCaseAnalysis(CaseAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        List<DocumentAnalysis> documentAnalyses = documentAnalysisRepository
                .findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE);

        if (documentAnalyses.isEmpty()) {
            log.warn("No DONE document analyses found for caseFile {} — case analysis skipped", caseFileId);
            return;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — case analysis skipped", caseFileId);
            return;
        }

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = caseAnalysisRepository.save(analysis);

        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = caseAnalysisRepository.save(analysis);

        try {
            String aggregatedPrompt = buildAggregatedPrompt(documentAnalyses);
            AnthropicResult result = anthropicService.analyzeChunk(aggregatedPrompt);
            analysis.setAnalysisResult(result.content());
            analysis.setModelUsed(result.modelUsed());
            analysis.setPromptTokens(result.promptTokens());
            analysis.setCompletionTokens(result.completionTokens());
            analysis.setAnalysisStatus(AnalysisStatus.DONE);
            log.info("Case analysis DONE for caseFile {}", caseFileId);
        } catch (Exception e) {
            log.error("Case analysis FAILED for caseFile {}", caseFileId, e);
            analysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }

        caseAnalysisRepository.save(analysis);
    }

    private String buildAggregatedPrompt(List<DocumentAnalysis> documentAnalyses) {
        List<DocumentAnalysis> sorted = documentAnalyses.stream()
                .sorted((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .toList();
        return IntStream.range(0, sorted.size())
                .mapToObj(i -> "Document %d : %s".formatted(i, sorted.get(i).getAnalysisResult()))
                .collect(Collectors.joining("\n"));
    }
}
