package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Profile({"local", "prod"})
public class CaseAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(CaseAnalysisService.class);

    static final String SYSTEM_PROMPT = """
            Tu es un assistant juridique expert en droit du travail français.
            Tu reçois les analyses de plusieurs documents d'un dossier juridique.
            Produis une synthèse globale du dossier en agrégeant ces analyses.
            Réponds UNIQUEMENT avec un objet JSON valide, sans texte avant ni après.
            Format attendu : {"timeline": [{"date": "YYYY-MM-DD", "evenement": "..."}], "faits": [...], "points_juridiques": [...], "risques": [...], "questions_ouvertes": [...]}
            La timeline doit lister les événements clés du dossier par ordre chronologique. Si aucune date n'est identifiable, utilise "timeline": [].
            Contraintes de longueur : 5 entrées timeline maximum, 7 faits maximum, 5 points_juridiques maximum, 5 risques maximum, 5 questions_ouvertes maximum. Sois concis.
            """;

    private final DocumentAnalysisRepository documentAnalysisRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final AnthropicService anthropicService;
    private final AnalysisJobRepository analysisJobRepository;
    private final RabbitTemplate rabbitTemplate;
    private final UsageEventService usageEventService;
    private final ApplicationEventPublisher eventPublisher;

    public CaseAnalysisService(DocumentAnalysisRepository documentAnalysisRepository,
                               CaseAnalysisRepository caseAnalysisRepository,
                               CaseFileRepository caseFileRepository,
                               AnthropicService anthropicService,
                               AnalysisJobRepository analysisJobRepository,
                               RabbitTemplate rabbitTemplate,
                               UsageEventService usageEventService,
                               ApplicationEventPublisher eventPublisher) {
        this.documentAnalysisRepository = documentAnalysisRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.anthropicService = anthropicService;
        this.analysisJobRepository = analysisJobRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.usageEventService = usageEventService;
        this.eventPublisher = eventPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.CASE_ANALYSIS_QUEUE)
    @Transactional
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

        AnalysisJob job = analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .orElseGet(() -> {
                    AnalysisJob j = new AnalysisJob();
                    j.setCaseFileId(caseFileId);
                    j.setJobType(JobType.CASE_ANALYSIS);
                    j.setProcessedItems(0);
                    return j;
                });
        job.setStatus(AnalysisStatus.PROCESSING);
        job.setTotalItems(1);
        analysisJobRepository.save(job);

        int nextVersion = caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId) + 1;

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setVersion(nextVersion);
        analysis.setAnalysisType(AnalysisType.STANDARD);
        analysis.setAnalysisStatus(AnalysisStatus.PENDING);
        analysis = caseAnalysisRepository.save(analysis);

        analysis.setAnalysisStatus(AnalysisStatus.PROCESSING);
        analysis = caseAnalysisRepository.save(analysis);

        try {
            String aggregatedPrompt = buildAggregatedPrompt(documentAnalyses);
            AnthropicResult result = anthropicService.analyze(SYSTEM_PROMPT, aggregatedPrompt, 8192);
            analysis.setAnalysisResult(AnalysisJsonTruncator.truncateCaseAnalysis(result.content()));
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

        job.setProcessedItems(1);
        job.setStatus(analysis.getAnalysisStatus());
        if (analysis.getAnalysisStatus() == AnalysisStatus.FAILED) {
            job.setErrorMessage("Case analysis failed");
        }
        analysisJobRepository.save(job);

        AnalysisStatus finalStatus = analysis.getAnalysisStatus();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                eventPublisher.publishEvent(new AnalysisStatusEvent(caseFileId, finalStatus));
                if (finalStatus == AnalysisStatus.DONE) {
                    rabbitTemplate.convertAndSend(
                            RabbitMQConfig.AI_QUESTION_GENERATION_EXCHANGE,
                            RabbitMQConfig.AI_QUESTION_GENERATION_ROUTING_KEY,
                            new AiQuestionGenerationMessage(caseFileId));
                }
            }
        });
        if (finalStatus == AnalysisStatus.DONE) {
            int promptTokens = analysis.getPromptTokens();
            int completionTokens = analysis.getCompletionTokens();
            caseFileRepository.findCreatedByUserIdById(caseFileId).ifPresent(userId ->
                usageEventService.record(caseFileId, userId, JobType.CASE_ANALYSIS,
                        promptTokens, completionTokens));
        }
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
