package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class EnrichedAnalysisServiceTest {

    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final UsageEventService usageEventService = mock(UsageEventService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final AnalysisDocumentSnapshotService analysisDocumentSnapshotService = mock(AnalysisDocumentSnapshotService.class);
    private final AnalysisQaSnapshotService analysisQaSnapshotService = mock(AnalysisQaSnapshotService.class);

    private final EnrichedAnalysisService service = new EnrichedAnalysisService(
            caseAnalysisRepository, caseFileRepository, aiQuestionRepository,
            aiQuestionAnswerRepository, analysisJobRepository, anthropicService, usageEventService, eventPublisher,
            analysisDocumentSnapshotService, analysisQaSnapshotService);

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

    // U-01 : nominal — synthèse enrichie DONE, job DONE
    @Test
    void consumeReAnalysis_nominal_persistsDoneAnalysisAndJob() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setAnalysisResult("{\"faits\":[\"fait1\"]}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);

        AiQuestion q = answeredQuestion(caseFileId, "Question ?");
        AiQuestionAnswer answer = new AiQuestionAnswer();
        answer.setAnswerText("Ma réponse");

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[\"enrichi\"]}", "claude-sonnet-4-6", 400, 200));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(captor.getValue().getAnalysisResult()).isEqualTo("{\"faits\":[\"enrichi\"]}");

        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, times(1)).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(jobCaptor.getValue().getProcessedItems()).isEqualTo(1);

        // Usage enregistré
        verify(usageEventService).record(caseFileId, userId, JobType.ENRICHED_ANALYSIS, 400, 200);
    }

    // U-02 : erreur LLM → analyse FAILED, job FAILED
    @Test
    void consumeReAnalysis_anthropicError_persistsFailedAnalysisAndJob() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setAnalysisResult("{}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyze(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);

        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(jobCaptor.getValue().getErrorMessage()).isNotNull();
    }

    // U-03 : pas de CaseAnalysis DONE → job FAILED, aucune analyse créée
    @Test
    void consumeReAnalysis_noPreviousAnalysis_jobFailed() {
        UUID caseFileId = UUID.randomUUID();

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.empty());

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        verify(caseAnalysisRepository, never()).save(any());
        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.FAILED);
    }

    // U-04 : buildEnrichedPrompt contient la synthèse précédente et les Q&R
    @Test
    void buildEnrichedPrompt_containsPreviousAnalysisAndQA() {
        UUID caseFileId = UUID.randomUUID();
        AiQuestion q = answeredQuestion(caseFileId, "Question test ?");
        AiQuestionAnswer answer = new AiQuestionAnswer();
        answer.setAnswerText("Réponse test");

        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        String prompt = service.buildEnrichedPrompt(caseFileId, "{\"faits\":[\"fait1\"]}");

        assertThat(prompt).contains("{\"faits\":[\"fait1\"]}");
        assertThat(prompt).contains("Question test ?");
        assertThat(prompt).contains("Réponse test");
        assertThat(prompt).contains("Synthèse précédente");
        assertThat(prompt).contains("Questions et réponses");
    }

    // U-06 : enriched analysis → analysisType = ENRICHED, version = max + 1
    @Test
    void consumeReAnalysis_setsEnrichedTypeAndIncrementsVersion() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setAnalysisResult("{}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findMaxVersionByCaseFileId(caseFileId)).thenReturn(1);
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(captor.capture());
        CaseAnalysis saved = captor.getAllValues().get(0);
        assertThat(saved.getAnalysisType()).isEqualTo(AnalysisType.ENRICHED);
        assertThat(saved.getVersion()).isEqualTo(2);
    }

    // U-05 : le system prompt contient les champs clés
    @Test
    void systemPrompt_containsRequiredFields() {
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).contains("timeline");
        assertThat(prompt).contains("faits");
        assertThat(prompt).contains("enrichie");
    }

    private AiQuestion answeredQuestion(UUID caseFileId, String text) {
        fr.ailegalcase.casefile.CaseFile cf = new fr.ailegalcase.casefile.CaseFile();
        AiQuestion q = new AiQuestion();
        q.setId(UUID.randomUUID());
        q.setCaseFile(cf);
        q.setQuestionText(text);
        q.setOrderIndex(0);
        q.setStatus("ANSWERED");
        return q;
    }
}
