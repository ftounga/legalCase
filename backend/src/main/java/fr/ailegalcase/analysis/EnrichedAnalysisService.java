package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class EnrichedAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(EnrichedAnalysisService.class);

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert en droit du travail français.
            Tu reçois la synthèse globale d'un dossier juridique ainsi que les réponses de l'avocat à des questions complémentaires.
            Produis une synthèse enrichie et mise à jour en intégrant ces nouvelles informations.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            """;

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;

    public EnrichedAnalysisService(CaseAnalysisRepository caseAnalysisRepository,
                                   CaseFileRepository caseFileRepository,
                                   AiQuestionRepository aiQuestionRepository,
                                   AiQuestionAnswerRepository aiQuestionAnswerRepository,
                                   AnalysisJobRepository analysisJobRepository,
                                   AnthropicService anthropicService,
                                   UsageEventService usageEventService) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.aiQuestionAnswerRepository = aiQuestionAnswerRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
    }

    @RabbitListener(queues = RabbitMQConfig.RE_ANALYSIS_QUEUE)
    @Transactional
    public void consumeReAnalysis(ReAnalysisMessage message) {
        UUID caseFileId = message.caseFileId();

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.ENRICHED_ANALYSIS);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    j.setStatus(AnalysisStatus.PROCESSING);
                    return j;
                });

        CaseAnalysis previousAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);

        if (previousAnalysis == null) {
            log.warn("No DONE case analysis found for caseFile {} — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("No previous case analysis found");
            analysisJobRepository.save(job);
            return;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — enriched analysis skipped", caseFileId);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Case file not found");
            analysisJobRepository.save(job);
            return;
        }

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis enrichedAnalysis = new CaseAnalysis();
        enrichedAnalysis.setCaseFile(caseFile);
        enrichedAnalysis.setVersion(nextVersion);
        enrichedAnalysis.setAnalysisType(AnalysisType.ENRICHED);
        enrichedAnalysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        enrichedAnalysis = caseAnalysisRepository.save(enrichedAnalysis);

        try {
            String prompt = buildEnrichedPrompt(caseFileId, previousAnalysis.getAnalysisResult());
            AnthropicResult result = anthropicService.analyze(SYSTEM_PROMPT, prompt, 8192);
            enrichedAnalysis.setAnalysisResult(AnalysisJsonTruncator.truncateCaseAnalysis(result.content()));
            enrichedAnalysis.setModelUsed(result.modelUsed());
            enrichedAnalysis.setPromptTokens(result.promptTokens());
            enrichedAnalysis.setCompletionTokens(result.completionTokens());
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
            log.info("Enriched analysis DONE for caseFile {}", caseFileId);
        } catch (Exception e) {
            log.error("Enriched analysis FAILED for caseFile {}", caseFileId, e);
            enrichedAnalysis.setAnalysisStatus(AnalysisStatus.FAILED);
        }

        caseAnalysisRepository.save(enrichedAnalysis);

        job.setProcessedItems(1);
        job.setStatus(enrichedAnalysis.getAnalysisStatus());
        if (enrichedAnalysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Enriched analysis failed");
        }
        analysisJobRepository.save(job);

        if (enrichedAnalysis.getAnalysisStatus() == AnalysisStatus.DONE) {
            int promptTokens = enrichedAnalysis.getPromptTokens();
            int completionTokens = enrichedAnalysis.getCompletionTokens();
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.ENRICHED_ANALYSIS,
                        promptTokens, completionTokens));
        }
    }

    String buildEnrichedPrompt(UUID caseFileId, String previousAnalysisResult) {
        List<AiQuestion> questions = aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId);

        List<AiQuestion> answeredQuestions = questions.stream()
                .filter(q -> "ANSWERED".equals(q.getStatus()))
                .toList();

        String qaSection = IntStream.range(0, answeredQuestions.size())
                .mapToObj(i -> {
                    AiQuestion q = answeredQuestions.get(i);
                    String answerText = aiQuestionAnswerRepository
                            .findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())
                            .map(AiQuestionAnswer::getAnswerText)
                            .orElse("(sans réponse)");
                    return "Q%d : %s\nR%d : %s".formatted(i + 1, q.getQuestionText(), i + 1, answerText);
                })
                .collect(Collectors.joining("\n"));

        return """
                [Synthèse précédente]
                %s

                [Questions et réponses de l'avocat]
                %s
                """.formatted(previousAnalysisResult, qaSection.isEmpty() ? "(aucune réponse)" : qaSection);
    }
}
