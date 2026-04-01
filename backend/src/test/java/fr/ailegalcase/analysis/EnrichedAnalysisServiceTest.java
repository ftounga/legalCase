package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.chat.ChatMessage;
import fr.ailegalcase.chat.ChatMessageRepository;
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
    private final AnalysisLimitsProperties analysisLimitsProperties = mock(AnalysisLimitsProperties.class);
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ProcedureCheckService procedureCheckService = mock(ProcedureCheckService.class);

    private final EnrichedAnalysisService service = new EnrichedAnalysisService(
            caseAnalysisRepository, caseFileRepository, aiQuestionRepository,
            aiQuestionAnswerRepository, analysisJobRepository, anthropicService, usageEventService, eventPublisher,
            analysisDocumentSnapshotService, analysisQaSnapshotService, analysisLimitsProperties,
            chatMessageRepository, procedureCheckService);

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(service, "self", service);
        AnalysisLimitsProperties.LevelLimits limits = new AnalysisLimitsProperties.LevelLimits();
        limits.setFaits(7); limits.setPointsJuridiques(5); limits.setRisques(5);
        limits.setQuestionsOuvertes(5); limits.setTimeline(5);
        AnalysisLimitsProperties.DomainLimits domainLimits = mock(AnalysisLimitsProperties.DomainLimits.class);
        when(domainLimits.getDossier()).thenReturn(limits);
        when(analysisLimitsProperties.forDomain(any())).thenReturn(domainLimits);
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

    // U-04 : buildEnrichedPrompt contient la synthèse précédente et les Q&R (sans résumé chat)
    @Test
    void buildEnrichedPrompt_containsPreviousAnalysisAndQA() {
        UUID caseFileId = UUID.randomUUID();
        AiQuestion q = answeredQuestion(caseFileId, "Question test ?");
        AiQuestionAnswer answer = new AiQuestionAnswer();
        answer.setAnswerText("Réponse test");

        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        String prompt = service.buildEnrichedPrompt(caseFileId, "{\"faits\":[\"fait1\"]}", null, List.of());

        assertThat(prompt).contains("{\"faits\":[\"fait1\"]}");
        assertThat(prompt).contains("Question test ?");
        assertThat(prompt).contains("Réponse test");
        assertThat(prompt).contains("Synthèse précédente");
        assertThat(prompt).contains("Questions et réponses");
        assertThat(prompt).doesNotContain("Échanges libres");
    }

    // U-07 : buildEnrichedPrompt avec résumé chat → section injectée
    @Test
    void buildEnrichedPrompt_withChatSummary_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", "Point clé 1 : délai de prescription dépassé", List.of());

        assertThat(prompt).contains("Échanges libres avec l'assistant — points clés");
        assertThat(prompt).contains("Point clé 1 : délai de prescription dépassé");
    }

    // U-08 : buildEnrichedPrompt avec résumé chat blanc → section absente
    @Test
    void buildEnrichedPrompt_withBlankChatSummary_omitsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", "   ", List.of());

        assertThat(prompt).doesNotContain("Échanges libres");
    }

    // U-09 : buildChatSummary — chat vide → null
    @Test
    void buildChatSummary_noMessages_returnsNull() {
        UUID caseFileId = UUID.randomUUID();
        when(chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId)).thenReturn(List.of());

        assertThat(service.buildChatSummary(caseFileId)).isNull();
        verify(anthropicService, never()).analyzeFast(any(), any(), anyInt());
    }

    // U-10 : buildChatSummary — erreur Haiku → fail-open, retourne null
    @Test
    void buildChatSummary_haikuError_returnsNullWithoutException() {
        UUID caseFileId = UUID.randomUUID();
        ChatMessage msg = new ChatMessage();
        msg.setQuestion("Question libre ?");
        msg.setAnswer("Réponse IA");
        when(chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId)).thenReturn(List.of(msg));
        when(anthropicService.analyzeFast(any(), any(), anyInt())).thenThrow(new RuntimeException("Haiku timeout"));

        assertThat(service.buildChatSummary(caseFileId)).isNull();
    }

    // U-11 : buildChatSummary — messages présents, Haiku répond → résumé retourné
    @Test
    void buildChatSummary_withMessages_returnsSummary() {
        UUID caseFileId = UUID.randomUUID();
        ChatMessage msg = new ChatMessage();
        msg.setQuestion("Quel est le délai ?");
        msg.setAnswer("Le délai est de 2 ans.");
        when(chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId)).thenReturn(List.of(msg));
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Délai de prescription : 2 ans", "claude-haiku-4-5", 50, 20));

        assertThat(service.buildChatSummary(caseFileId)).isEqualTo("Délai de prescription : 2 ans");
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
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).contains("timeline");
        assertThat(prompt).contains("faits");
        assertThat(prompt).contains("enrichie");
    }

    // TC-01 : buildEnrichedPrompt avec checks NON_COMPLIANT → section injectée
    @Test
    void buildEnrichedPrompt_withNonCompliantChecks_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        List<String> checks = List.of("Entretien préalable non convoqué par LRAR", "Notification hors délai");
        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, checks);

        assertThat(prompt).contains("[Points procéduraux non conformes]");
        assertThat(prompt).contains("- Entretien préalable non convoqué par LRAR");
        assertThat(prompt).contains("- Notification hors délai");
    }

    // TC-02 : buildEnrichedPrompt sans checks NON_COMPLIANT → section absente
    @Test
    void buildEnrichedPrompt_withNoNonCompliantChecks_omitsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, List.of());

        assertThat(prompt).doesNotContain("[Points procéduraux non conformes]");
    }

    // TC-03 : listNonCompliant lève exception → fail-open, re-analyse sans la section
    @Test
    void prepareEnrichedAnalysis_listNonCompliantThrows_proceedsWithoutSection() {
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
        when(procedureCheckService.listNonCompliant(any())).thenThrow(new RuntimeException("DB error"));
        when(chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId)).thenReturn(List.of());

        var prepared = service.prepareEnrichedAnalysis(new ReAnalysisMessage(caseFileId));

        assertThat(prepared).isNotNull();
        assertThat(prepared.prompt()).doesNotContain("[Points procéduraux non conformes]");
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
