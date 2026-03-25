package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
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
import static org.mockito.Mockito.*;

class SentryJobReportingTest {

    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final UsageEventService usageEventService = mock(UsageEventService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final CaseAnalysisService caseAnalysisService = new CaseAnalysisService(
            documentAnalysisRepository, caseAnalysisRepository, caseFileRepository,
            anthropicService, analysisJobRepository, rabbitTemplate, usageEventService, eventPublisher);

    private final AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);

    private final EnrichedAnalysisService enrichedAnalysisService = new EnrichedAnalysisService(
            caseAnalysisRepository, caseFileRepository, aiQuestionRepository,
            aiQuestionAnswerRepository, analysisJobRepository, anthropicService, usageEventService, eventPublisher);

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(caseAnalysisService, "self", caseAnalysisService);
        ReflectionTestUtils.setField(enrichedAnalysisService, "self", enrichedAnalysisService);
        when(caseAnalysisRepository.findById(any())).thenAnswer(inv -> {
            CaseAnalysis a = new CaseAnalysis();
            a.setAnalysisStatus(AnalysisStatus.PROCESSING);
            return Optional.of(a);
        });
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // S-01 : CaseAnalysis FAILED + Sentry activé → captureEvent appelé avec les bons tags
    @Test
    void caseAnalysis_failed_reportsSentryEventWithCorrectTags() {
        UUID caseFileId = UUID.randomUUID();
        setupCaseAnalysisFailure(caseFileId);

        try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
            sentry.when(Sentry::isEnabled).thenReturn(true);

            caseAnalysisService.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

            ArgumentCaptor<SentryEvent> captor = ArgumentCaptor.forClass(SentryEvent.class);
            sentry.verify(() -> Sentry.captureEvent(captor.capture()));
            SentryEvent event = captor.getValue();
            assertThat(event.getTag("caseFileId")).isEqualTo(caseFileId.toString());
            assertThat(event.getTag("jobType")).isEqualTo(JobType.CASE_ANALYSIS.name());
        }
    }

    // S-02 : CaseAnalysis DONE → captureEvent non appelé
    @Test
    void caseAnalysis_done_doesNotReportToSentry() {
        UUID caseFileId = UUID.randomUUID();
        setupCaseAnalysisSuccess(caseFileId);

        try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
            sentry.when(Sentry::isEnabled).thenReturn(true);

            caseAnalysisService.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

            sentry.verify(() -> Sentry.captureEvent(any()), never());
        }
    }

    // S-03 : EnrichedAnalysis FAILED + Sentry activé → captureEvent appelé avec les bons tags
    @Test
    void enrichedAnalysis_failed_reportsSentryEventWithCorrectTags() {
        UUID caseFileId = UUID.randomUUID();
        setupEnrichedAnalysisFailure(caseFileId);

        try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
            sentry.when(Sentry::isEnabled).thenReturn(true);

            enrichedAnalysisService.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

            ArgumentCaptor<SentryEvent> captor = ArgumentCaptor.forClass(SentryEvent.class);
            sentry.verify(() -> Sentry.captureEvent(captor.capture()));
            SentryEvent event = captor.getValue();
            assertThat(event.getTag("caseFileId")).isEqualTo(caseFileId.toString());
            assertThat(event.getTag("jobType")).isEqualTo(JobType.ENRICHED_ANALYSIS.name());
        }
    }

    // S-04 : Sentry désactivé → captureEvent non appelé (fail-open, pas d'exception)
    @Test
    void caseAnalysis_failed_sentryDisabled_doesNotThrow() {
        UUID caseFileId = UUID.randomUUID();
        setupCaseAnalysisFailure(caseFileId);

        try (MockedStatic<Sentry> sentry = mockStatic(Sentry.class)) {
            sentry.when(Sentry::isEnabled).thenReturn(false);

            caseAnalysisService.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

            sentry.verify(() -> Sentry.captureEvent(any()), never());
        }
    }

    // --- helpers ---

    private void setupCaseAnalysisFailure(UUID caseFileId) {
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());
        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(new CaseFile()));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyze(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private void setupCaseAnalysisSuccess(UUID caseFileId) {
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());
        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(new CaseFile()));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyze(any(), any(), anyInt()))
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
        when(anthropicService.analyze(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));
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
