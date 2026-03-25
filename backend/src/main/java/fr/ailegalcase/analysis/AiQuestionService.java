package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Profile({"local", "prod"})
public class AiQuestionService {

    private static final Logger log = LoggerFactory.getLogger(AiQuestionService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert en droit du travail français.
            Tu reçois la synthèse globale d'un dossier juridique.
            Génère une liste de questions complémentaires pour l'avocat afin d'approfondir l'analyse.
            Ces questions doivent porter sur des éléments manquants, des ambiguïtés ou des points à clarifier.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"questions": ["Question 1 ?", "Question 2 ?"]}
            Génère entre 3 et 8 questions.
            """;

    record PreparedQuestionGeneration(String prompt, UUID caseFileId, UUID caseAnalysisId) {}

    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AiQuestionRepository aiQuestionRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final AnthropicService anthropicService;
    private final UsageEventService usageEventService;

    @Lazy @Autowired
    private AiQuestionService self;

    public AiQuestionService(CaseAnalysisRepository caseAnalysisRepository,
                             CaseFileRepository caseFileRepository,
                             AiQuestionRepository aiQuestionRepository,
                             AnalysisJobRepository analysisJobRepository,
                             AnthropicService anthropicService,
                             UsageEventService usageEventService) {
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.aiQuestionRepository = aiQuestionRepository;
        this.analysisJobRepository = analysisJobRepository;
        this.anthropicService = anthropicService;
        this.usageEventService = usageEventService;
    }

    @RabbitListener(queues = RabbitMQConfig.AI_QUESTION_GENERATION_QUEUE, concurrency = "3")
    public void consumeQuestionGeneration(AiQuestionGenerationMessage message) {
        long startMs = System.currentTimeMillis();
        UUID caseFileId = message.caseFileId();

        PreparedQuestionGeneration prepared = self.prepareQuestionGeneration(message);
        if (prepared == null) return;

        AnthropicResult result = null;
        Exception failure = null;
        try {
            log.info("Question generation START for caseFile {} ({} chars)", caseFileId, prepared.prompt().length());
            long anthropicStart = System.currentTimeMillis();
            result = anthropicService.analyze(SYSTEM_PROMPT, prepared.prompt(), 1024);
            long anthropicMs = System.currentTimeMillis() - anthropicStart;
            log.info("Question generation DONE for caseFile {} — Anthropic {}ms, total {}ms, tokens {}/{}",
                    caseFileId, anthropicMs, System.currentTimeMillis() - startMs,
                    result.promptTokens(), result.completionTokens());
        } catch (Exception e) {
            log.error("Question generation FAILED for caseFile {} (total {}ms)", caseFileId,
                    System.currentTimeMillis() - startMs, e);
            failure = e;
        }

        self.finalizeQuestionGeneration(prepared.caseFileId(), prepared.caseAnalysisId(), result, failure);
    }

    @Transactional
    public PreparedQuestionGeneration prepareQuestionGeneration(AiQuestionGenerationMessage message) {
        UUID caseFileId = message.caseFileId();

        CaseAnalysis caseAnalysis = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);

        if (caseAnalysis == null) {
            log.warn("No DONE case analysis found for caseFile {} — question generation skipped", caseFileId);
            return null;
        }

        CaseFile caseFile = caseFileRepository.findById(caseFileId).orElse(null);
        if (caseFile == null) {
            log.error("CaseFile {} not found — question generation skipped", caseFileId);
            return null;
        }

        AnalysisJob job = analysisJobRepository
                .findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.QUESTION_GENERATION);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        return new PreparedQuestionGeneration(caseAnalysis.getAnalysisResult(), caseFileId, caseAnalysis.getId());
    }

    @Transactional
    public void finalizeQuestionGeneration(UUID caseFileId, UUID caseAnalysisId, AnthropicResult result, Exception failure) {
        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.QUESTION_GENERATION);
                    j.setTotalItems(1);
                    j.setProcessedItems(0);
                    return j;
                });

        if (failure != null) {
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Question generation failed");
            analysisJobRepository.save(job);
            return;
        }

        try {
            List<String> questions = parseQuestions(result.content());
            CaseFile caseFile = caseFileRepository.findById(caseFileId).orElseThrow();
            CaseAnalysis caseAnalysis = caseAnalysisRepository.findById(caseAnalysisId).orElseThrow();

            for (int i = 0; i < questions.size(); i++) {
                AiQuestion question = new AiQuestion();
                question.setCaseFile(caseFile);
                question.setCaseAnalysis(caseAnalysis);
                question.setQuestionText(questions.get(i));
                question.setOrderIndex(i);
                aiQuestionRepository.save(question);
            }

            job.setProcessedItems(1);
            job.setStatus(AnalysisStatus.DONE);
            log.info("Question generation finalized for caseFile {} — {} questions", caseFileId, questions.size());
        } catch (Exception e) {
            log.error("Question generation finalization FAILED for caseFile {}", caseFileId, e);
            job.setStatus(AnalysisStatus.FAILED);
            job.setErrorMessage("Question generation failed");
        }

        analysisJobRepository.save(job);

        if (job.getStatus() == AnalysisStatus.DONE) {
            final AnthropicResult finalResult = result;
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.QUESTION_GENERATION,
                        finalResult.promptTokens(), finalResult.completionTokens()));
        }
    }

    static List<String> parseQuestions(String json) {
        try {
            JsonNode root = MAPPER.readTree(CaseAnalysisResponse.stripMarkdownCodeBlock(json));
            JsonNode questionsNode = root.get("questions");
            if (questionsNode == null || !questionsNode.isArray()) return List.of();
            List<String> result = new ArrayList<>();
            for (JsonNode item : questionsNode) {
                if (item.isTextual()) result.add(item.asText());
            }
            return List.copyOf(result);
        } catch (Exception e) {
            return List.of();
        }
    }
}
