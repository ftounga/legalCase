package fr.ailegalcase.analysis;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fr.ailegalcase.casefile.CaseDeadlineService;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * SF-INFRA-09 — remplace SentryJobReportingTest.
 *
 * <p>Vérifie que lorsqu'un job IA passe à FAILED, une ligne SLF4J ERROR est
 * émise avec les bons champs (caseFileId + jobType + errorMessage). Cette ligne
 * sera captée par Fluent Bit → metric filter "ERROR" CloudWatch → alarme
 * legalcase-production-backend-error-rate.</p>
 */
class JobFailureLoggingTest {

    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final UsageEventService usageEventService = mock(UsageEventService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService = mock(AnalysisDocumentSnapshotService.class);
    private final AnalysisLimitsProperties analysisLimitsProperties = mock(AnalysisLimitsProperties.class);

    private final ProcedureCheckService procedureCheckService = mock(ProcedureCheckService.class);
    private final CaseDeadlineService caseDeadlineService = mock(CaseDeadlineService.class);
    private final SourceExplanationGenerator sourceExplanationGenerator = mock(SourceExplanationGenerator.class);
    private final SourceExplanationService sourceExplanationService = mock(SourceExplanationService.class);

    private final fr.ailegalcase.document.DocumentExtractionRepository documentExtractionRepository =
            mock(fr.ailegalcase.document.DocumentExtractionRepository.class);
    private final PiecesPromptContext piecesPromptContext = mock(PiecesPromptContext.class);
    private final StrategicOptionService strategicOptionService = mock(StrategicOptionService.class);
    private final JurisprudenceVerificationService jurisprudenceVerificationService =
            mock(JurisprudenceVerificationService.class);

    private final CaseAnalysisService caseAnalysisService = new CaseAnalysisService(
            documentAnalysisRepository, documentExtractionRepository, caseAnalysisRepository, caseFileRepository,
            anthropicService, analysisJobRepository, rabbitTemplate, usageEventService, eventPublisher,
            analysisDocumentSnapshotService, analysisLimitsProperties, procedureCheckService, strategicOptionService, caseDeadlineService,
            sourceExplanationGenerator, sourceExplanationService, piecesPromptContext, jurisprudenceVerificationService);

    private final AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);
    private final AnalysisQaSnapshotService analysisQaSnapshotService = mock(AnalysisQaSnapshotService.class);
    private final fr.ailegalcase.chat.ChatMessageRepository chatMessageRepository = mock(fr.ailegalcase.chat.ChatMessageRepository.class);

    private final fr.ailegalcase.casefile.StatutoryDeadlineService statutoryDeadlineService =
            mock(fr.ailegalcase.casefile.StatutoryDeadlineService.class);
    private final fr.ailegalcase.referential.LegalReferentialService legalReferentialService =
            mock(fr.ailegalcase.referential.LegalReferentialService.class);

    private final fr.ailegalcase.document.DocumentRepository documentRepository =
            mock(fr.ailegalcase.document.DocumentRepository.class);

    private final RetainedPisteAlignmentService retainedPisteAlignmentService =
            mock(RetainedPisteAlignmentService.class);
    private final ProcedureCheckAlignmentService procedureCheckAlignmentService =
            mock(ProcedureCheckAlignmentService.class);
    private final PieceManquanteAlignmentService pieceManquanteAlignmentService =
            mock(PieceManquanteAlignmentService.class);
    private final PieceManquanteStatusService pieceManquanteStatusService =
            mock(PieceManquanteStatusService.class);
    private final RisqueAlignmentService risqueAlignmentService =
            mock(RisqueAlignmentService.class);
    private final RisqueStatusService risqueStatusService =
            mock(RisqueStatusService.class);
    private final AiQuestionAlignmentService aiQuestionAlignmentService =
            mock(AiQuestionAlignmentService.class);
    private final TypeLitigeOverrideService typeLitigeOverrideService =
            mock(TypeLitigeOverrideService.class);

    private final EnrichedAnalysisService enrichedAnalysisService = new EnrichedAnalysisService(
            caseAnalysisRepository, caseFileRepository, aiQuestionRepository,
            aiQuestionAnswerRepository, analysisJobRepository, anthropicService, usageEventService, eventPublisher,
            analysisDocumentSnapshotService, analysisQaSnapshotService, analysisLimitsProperties,
            chatMessageRepository, procedureCheckService, strategicOptionService, retainedPisteAlignmentService,
            procedureCheckAlignmentService,
            pieceManquanteAlignmentService, pieceManquanteStatusService,
            risqueAlignmentService, risqueStatusService,
            aiQuestionAlignmentService,
            typeLitigeOverrideService,
            statutoryDeadlineService, legalReferentialService,
            sourceExplanationGenerator, sourceExplanationService,
            jurisprudenceVerificationService,
            documentRepository, documentExtractionRepository, documentAnalysisRepository, piecesPromptContext);

    private ListAppender<ILoggingEvent> caseAppender;
    private ListAppender<ILoggingEvent> enrichedAppender;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(caseAnalysisService, "self", caseAnalysisService);
        ReflectionTestUtils.setField(enrichedAnalysisService, "self", enrichedAnalysisService);
        AnalysisLimitsProperties.LevelLimits limits = new AnalysisLimitsProperties.LevelLimits();
        limits.setFaits(5); limits.setPointsJuridiques(3); limits.setRisques(3);
        limits.setQuestionsOuvertes(3); limits.setTimeline(5);
        AnalysisLimitsProperties.DomainLimits domainLimits = mock(AnalysisLimitsProperties.DomainLimits.class);
        when(domainLimits.getDossier()).thenReturn(limits);
        when(analysisLimitsProperties.forDomain(any())).thenReturn(domainLimits);
        when(caseAnalysisRepository.findById(any())).thenAnswer(inv -> {
            CaseAnalysis a = new CaseAnalysis();
            a.setAnalysisStatus(AnalysisStatus.PROCESSING);
            return Optional.of(a);
        });
        when(pieceManquanteStatusService.collectForEnrichment(any()))
                .thenReturn(PieceManquanteStatusService.EnrichmentSnapshot.empty());
        when(risqueStatusService.collectForEnrichment(any()))
                .thenReturn(RisqueStatusService.EnrichmentSnapshot.empty());

        // Branche un ListAppender sur les loggers des deux services pour capturer
        // les events ERROR émis par logJobFailure(...).
        caseAppender = attachAppender(CaseAnalysisService.class);
        enrichedAppender = attachAppender(EnrichedAnalysisService.class);
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
        detachAppender(CaseAnalysisService.class, caseAppender);
        detachAppender(EnrichedAnalysisService.class, enrichedAppender);
    }

    // J-01 : CaseAnalysis FAILED → log.error SLF4J émis avec caseFileId + jobType
    @Test
    void caseAnalysis_failed_emitsErrorLogWithCaseFileIdAndJobType() {
        UUID caseFileId = UUID.randomUUID();
        setupCaseAnalysisFailure(caseFileId);

        caseAnalysisService.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ILoggingEvent failureEvent = findFailureEvent(caseAppender);
        assertThat(failureEvent).as("expected a log.error containing job failure").isNotNull();
        assertThat(failureEvent.getLevel()).isEqualTo(Level.ERROR);
        String formatted = failureEvent.getFormattedMessage();
        assertThat(formatted).contains(caseFileId.toString());
        assertThat(formatted).contains(JobType.CASE_ANALYSIS.name());
        assertThat(formatted).contains("Case analysis failed");
    }

    // J-02 : CaseAnalysis DONE → aucun log.error job-failure
    @Test
    void caseAnalysis_done_doesNotEmitErrorLog() {
        UUID caseFileId = UUID.randomUUID();
        setupCaseAnalysisSuccess(caseFileId);

        caseAnalysisService.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        assertThat(findFailureEvent(caseAppender)).as("DONE should not produce a job-failure ERROR log").isNull();
    }

    // J-03 : EnrichedAnalysis FAILED → log.error SLF4J émis avec caseFileId + jobType
    @Test
    void enrichedAnalysis_failed_emitsErrorLogWithCaseFileIdAndJobType() {
        UUID caseFileId = UUID.randomUUID();
        setupEnrichedAnalysisFailure(caseFileId);

        enrichedAnalysisService.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        ILoggingEvent failureEvent = findFailureEvent(enrichedAppender);
        assertThat(failureEvent).as("expected a log.error containing job failure").isNotNull();
        assertThat(failureEvent.getLevel()).isEqualTo(Level.ERROR);
        String formatted = failureEvent.getFormattedMessage();
        assertThat(formatted).contains(caseFileId.toString());
        assertThat(formatted).contains(JobType.ENRICHED_ANALYSIS.name());
    }

    // --- helpers ---

    private static ListAppender<ILoggingEvent> attachAppender(Class<?> target) {
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Logger logger = (Logger) LoggerFactory.getLogger(target);
        logger.addAppender(appender);
        return appender;
    }

    private static void detachAppender(Class<?> target, ListAppender<ILoggingEvent> appender) {
        if (appender == null) return;
        Logger logger = (Logger) LoggerFactory.getLogger(target);
        logger.detachAppender(appender);
    }

    private static ILoggingEvent findFailureEvent(ListAppender<ILoggingEvent> appender) {
        return appender.list.stream()
                .filter(e -> e.getLevel() == Level.ERROR)
                .filter(e -> {
                    String msg = e.getFormattedMessage();
                    return msg != null && msg.contains("IA job FAILED");
                })
                .findFirst()
                .orElse(null);
    }

    private void setupCaseAnalysisFailure(UUID caseFileId) {
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());
        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(new CaseFile()));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupCaseAnalysisSuccess(UUID caseFileId) {
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());
        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(new CaseFile()));
        // F-257 — pré-résolution user-level pour AiCallContext (sinon SKIP early → log ERROR + status FAILED)
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(UUID.randomUUID()));
        when(caseFileRepository.findWorkspaceIdById(caseFileId)).thenReturn(Optional.of(UUID.randomUUID()));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupEnrichedAnalysisFailure(UUID caseFileId) {
        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setAnalysisResult("{}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(new CaseFile()));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));
    }

    private DocumentAnalysis documentAnalysis(String result, Instant createdAt) {
        DocumentAnalysis da = new DocumentAnalysis();
        da.setDocument(new Document());
        da.setAnalysisResult(result);
        da.setAnalysisStatus(AnalysisStatus.DONE);
        da.setCreatedAt(createdAt);
        return da;
    }
}
