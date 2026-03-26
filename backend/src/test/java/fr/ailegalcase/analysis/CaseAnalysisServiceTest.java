package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.document.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.*;

class CaseAnalysisServiceTest {

    private final DocumentAnalysisRepository documentAnalysisRepository = mock(DocumentAnalysisRepository.class);
    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final UsageEventService usageEventService = mock(UsageEventService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService = mock(AnalysisDocumentSnapshotService.class);

    private final CaseAnalysisService service = new CaseAnalysisService(
            documentAnalysisRepository, caseAnalysisRepository, caseFileRepository,
            anthropicService, analysisJobRepository, rabbitTemplate, usageEventService, eventPublisher,
            analysisDocumentSnapshotService);

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(service, "self", service);
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

    // U-01 : analyses de documents valides → CaseAnalysis DONE + job DONE
    @Test
    void consumeCaseAnalysis_validDocumentAnalyses_persistsDoneAnalysisAndJob() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        DocumentAnalysis da0 = documentAnalysis("{\"faits\":[\"fait1\"]}", Instant.now().minusSeconds(10));
        DocumentAnalysis da1 = documentAnalysis("{\"faits\":[\"fait2\"]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da0, da1));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[\"synthese\"]}", "claude-sonnet-4-6", 300, 150));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(captor.getValue().getAnalysisResult()).isEqualTo("{\"faits\":[\"synthese\"]}");
        assertThat(captor.getValue().getModelUsed()).isEqualTo("claude-sonnet-4-6");
        assertThat(captor.getValue().getPromptTokens()).isEqualTo(300);
        assertThat(captor.getValue().getCompletionTokens()).isEqualTo(150);

        // Job mis à jour : DONE, processedItems=1
        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, times(2)).save(jobCaptor.capture()); // PROCESSING + DONE
        AnalysisJob finalJob = jobCaptor.getValue();
        assertThat(finalJob.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(finalJob.getProcessedItems()).isEqualTo(1);

        // Usage enregistré
        verify(usageEventService).record(caseFileId, userId, JobType.CASE_ANALYSIS, 300, 150);
    }

    // U-02 : erreur Anthropic → CaseAnalysis FAILED + job FAILED
    @Test
    void consumeCaseAnalysis_anthropicError_persistsFailedAnalysisAndJob() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyze(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(3)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);

        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, times(2)).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(jobCaptor.getValue().getErrorMessage()).isNotNull();
    }

    // U-03 : CASE_ANALYSIS DONE → publie un message AiQuestionGenerationMessage
    @Test
    void consumeCaseAnalysis_done_publishesQuestionGenerationMessage() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));
        // Simulate transaction commit to trigger afterCommit callbacks
        TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.AI_QUESTION_GENERATION_EXCHANGE,
                RabbitMQConfig.AI_QUESTION_GENERATION_ROUTING_KEY,
                new AiQuestionGenerationMessage(caseFileId));
    }

    // U-04 : aucune document_analysis DONE → aucune CaseAnalysis créée
    @Test
    void consumeCaseAnalysis_noDocumentAnalyses_noCaseAnalysisCreated() {
        UUID caseFileId = UUID.randomUUID();
        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of());

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        verifyNoInteractions(caseAnalysisRepository, anthropicService, caseFileRepository, analysisJobRepository);
    }

    // U-04 : le system prompt contient le champ timeline
    @Test
    void systemPrompt_containsTimelineField() {
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).contains("timeline");
        assertThat(prompt).contains("date");
        assertThat(prompt).contains("evenement");
    }

    // U-06 : le prompt agrège les documents dans l'ordre chronologique
    @Test
    void consumeCaseAnalysis_promptContainsDocumentsInChronologicalOrder() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        Instant earlier = Instant.now().minusSeconds(60);
        Instant later = Instant.now();

        DocumentAnalysis daLater = documentAnalysis("{\"faits\":[\"B\"]}", later);
        DocumentAnalysis daEarlier = documentAnalysis("{\"faits\":[\"A\"]}", earlier);

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(daLater, daEarlier)); // ordre inversé intentionnel
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyze(any(), promptCaptor.capture(), anyInt());
        String prompt = promptCaptor.getValue();
        assertThat(prompt.indexOf("Document 0")).isLessThan(prompt.indexOf("Document 1"));
        assertThat(prompt).contains("{\"faits\":[\"A\"]}");
        assertThat(prompt.indexOf("{\"faits\":[\"A\"]}")).isLessThan(prompt.indexOf("{\"faits\":[\"B\"]}"));
    }

    // U-07 : le system prompt contient les contraintes de longueur SF-28-01
    @Test
    void systemPrompt_containsLengthConstraints() {
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).contains("5 entrées timeline maximum");
        assertThat(prompt).contains("7 faits maximum");
        assertThat(prompt).contains("5 points_juridiques maximum");
        assertThat(prompt).contains("5 risques maximum");
        assertThat(prompt).contains("5 questions_ouvertes maximum");
    }

    // U-08 : première analyse → version = 1, analysisType = STANDARD
    @Test
    void consumeCaseAnalysis_firstAnalysis_setsVersionOneAndStandardType() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId)).thenReturn(0);
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(captor.capture());
        CaseAnalysis saved = captor.getAllValues().get(0);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getAnalysisType()).isEqualTo(AnalysisType.STANDARD);
    }

    // U-09 : re-analyse → version = max + 1
    @Test
    void consumeCaseAnalysis_reAnalysis_incrementsVersion() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId)).thenReturn(2);
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getVersion()).isEqualTo(3);
    }

    // Helper
    private DocumentAnalysis documentAnalysis(String result, Instant createdAt) {
        DocumentAnalysis da = new DocumentAnalysis();
        da.setDocument(new Document());
        da.setAnalysisResult(result);
        da.setAnalysisStatus(AnalysisStatus.DONE);
        da.setCreatedAt(createdAt);
        return da;
    }
}
