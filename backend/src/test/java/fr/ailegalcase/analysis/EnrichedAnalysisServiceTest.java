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
    private final fr.ailegalcase.casefile.StatutoryDeadlineService statutoryDeadlineService =
            mock(fr.ailegalcase.casefile.StatutoryDeadlineService.class);
    private final fr.ailegalcase.referential.LegalReferentialService legalReferentialService =
            mock(fr.ailegalcase.referential.LegalReferentialService.class);
    private final SourceExplanationGenerator sourceExplanationGenerator = mock(SourceExplanationGenerator.class);
    private final SourceExplanationService sourceExplanationService = mock(SourceExplanationService.class);
    private final fr.ailegalcase.document.DocumentRepository documentRepository =
            mock(fr.ailegalcase.document.DocumentRepository.class);
    private final fr.ailegalcase.document.DocumentExtractionRepository documentExtractionRepository =
            mock(fr.ailegalcase.document.DocumentExtractionRepository.class);
    private final PiecesPromptContext piecesPromptContext = mock(PiecesPromptContext.class);

    private final EnrichedAnalysisService service = new EnrichedAnalysisService(
            caseAnalysisRepository, caseFileRepository, aiQuestionRepository,
            aiQuestionAnswerRepository, analysisJobRepository, anthropicService, usageEventService, eventPublisher,
            analysisDocumentSnapshotService, analysisQaSnapshotService, analysisLimitsProperties,
            chatMessageRepository, procedureCheckService, statutoryDeadlineService, legalReferentialService,
            sourceExplanationGenerator, sourceExplanationService,
            documentRepository, documentExtractionRepository, piecesPromptContext);

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
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
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
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

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

        String prompt = service.buildEnrichedPrompt(caseFileId, "{\"faits\":[\"fait1\"]}", null, List.of(), List.of(), List.of());

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

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", "Point clé 1 : délai de prescription dépassé", List.of(), List.of(), List.of());

        assertThat(prompt).contains("Échanges libres avec l'assistant — points clés");
        assertThat(prompt).contains("Point clé 1 : délai de prescription dépassé");
    }

    // U-08 : buildEnrichedPrompt avec résumé chat blanc → section absente
    @Test
    void buildEnrichedPrompt_withBlankChatSummary_omitsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", "   ", List.of(), List.of(), List.of());

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
        when(procedureCheckService.listNonCompliant(any())).thenReturn(List.of());
        when(procedureCheckService.listToCheck(any())).thenReturn(List.of());
        when(procedureCheckService.listVerified(any())).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
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
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l,
                java.util.List.of("LICENCIEMENT_SANS_CAUSE_REELLE", "HARCELEMENT_MORAL"));
        assertThat(prompt).contains("timeline");
        assertThat(prompt).contains("faits");
        assertThat(prompt).contains("enrichie");
    }

    // SF-128-01 : règle de préservation baseline généralisée dans le prompt enrichi
    @Test
    void systemPrompt_containsBaselinePreservationRuleForAllDomains() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l,
                java.util.List.of("LICENCIEMENT_SANS_CAUSE_REELLE"));
        assertThat(prompt).contains("RÈGLE CRITIQUE DE PRÉSERVATION BASELINE");
        // Les 3 domaines cités avec leurs champs
        assertThat(prompt).contains("type_rupture");
        assertThat(prompt).contains("type_titre_sejour_code");
        assertThat(prompt).contains("regime_matrimonial");
        assertThat(prompt).contains("mode_garde_detaille");
    }

    // TC-01 : buildEnrichedPrompt avec checks NON_COMPLIANT → section injectée
    @Test
    void buildEnrichedPrompt_withNonCompliantChecks_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        List<String> checks = List.of("Entretien préalable non convoqué par LRAR", "Notification hors délai");
        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, checks, List.of(), List.of());

        assertThat(prompt).contains("[Points procéduraux non conformes]");
        assertThat(prompt).contains("- Entretien préalable non convoqué par LRAR");
        assertThat(prompt).contains("- Notification hors délai");
    }

    // TC-02 : buildEnrichedPrompt sans checks NON_COMPLIANT → section absente
    @Test
    void buildEnrichedPrompt_withNoNonCompliantChecks_omitsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, List.of(), List.of(), List.of());

        assertThat(prompt).doesNotContain("[Points procéduraux non conformes]");
    }

    // TC-04 : buildEnrichedPrompt avec checks TO_CHECK → section injectée
    @Test
    void buildEnrichedPrompt_withToCheckChecks_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        List<String> toCheck = List.of("Entretien préalable tenu dans les délais", "Convocation par LRAR");
        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, List.of(), toCheck, List.of());

        assertThat(prompt).contains("[Points procéduraux à vérifier — non encore qualifiés par l'avocat]");
        assertThat(prompt).contains("- Entretien préalable tenu dans les délais");
        assertThat(prompt).contains("- Convocation par LRAR");
    }

    // TC-05 : buildEnrichedPrompt sans TO_CHECK → section absente
    @Test
    void buildEnrichedPrompt_withNoToCheckChecks_omitsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, List.of(), List.of(), List.of());

        assertThat(prompt).doesNotContain("[Points procéduraux à vérifier");
    }

    // TC-06 : buildEnrichedPrompt avec NON_COMPLIANT + TO_CHECK → les deux sections présentes et distinctes
    @Test
    void buildEnrichedPrompt_withBothNonCompliantAndToCheck_injectsBothSections() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        List<String> nonCompliant = List.of("Lettre de licenciement non motivée");
        List<String> toCheck = List.of("Entretien préalable dans les délais");
        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, nonCompliant, toCheck, List.of());

        assertThat(prompt).contains("[Points procéduraux non conformes]");
        assertThat(prompt).contains("- Lettre de licenciement non motivée");
        assertThat(prompt).contains("[Points procéduraux à vérifier — non encore qualifiés par l'avocat]");
        assertThat(prompt).contains("- Entretien préalable dans les délais");
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
        when(procedureCheckService.listToCheck(any())).thenReturn(List.of());
        when(procedureCheckService.listVerified(any())).thenReturn(List.of());
        when(chatMessageRepository.findByCaseFileIdOrderByCreatedAtAsc(caseFileId)).thenReturn(List.of());

        var prepared = service.prepareEnrichedAnalysis(new ReAnalysisMessage(caseFileId));

        assertThat(prepared).isNotNull();
        assertThat(prepared.prompt()).doesNotContain("[Points procéduraux non conformes]");
    }

    // TC-07 : buildEnrichedPrompt avec VERIFIED → section injectée
    @Test
    void buildEnrichedPrompt_withVerifiedChecks_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        List<String> verified = List.of("Entretien préalable tenu dans les délais", "Convocation par LRAR");
        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, List.of(), List.of(), verified);

        assertThat(prompt).contains("[Points procéduraux vérifiés — à reconsidérer si nécessaire]");
        assertThat(prompt).contains("- Entretien préalable tenu dans les délais");
        assertThat(prompt).contains("- Convocation par LRAR");
    }

    // TC-08 : buildEnrichedPrompt sans VERIFIED → section absente
    @Test
    void buildEnrichedPrompt_withNoVerifiedChecks_omitsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, List.of(), List.of(), List.of());

        assertThat(prompt).doesNotContain("[Points procéduraux vérifiés");
    }

    // TC-09 : buildEnrichedPrompt avec NON_COMPLIANT + TO_CHECK + VERIFIED → 3 sections distinctes
    @Test
    void buildEnrichedPrompt_withAllThreeTypes_injectsThreeSections() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        List<String> nonCompliant = List.of("Lettre de licenciement non motivée");
        List<String> toCheck = List.of("Entretien préalable dans les délais");
        List<String> verified = List.of("Convocation par LRAR envoyée");
        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null, nonCompliant, toCheck, verified);

        assertThat(prompt).contains("[Points procéduraux non conformes]");
        assertThat(prompt).contains("[Points procéduraux à vérifier — non encore qualifiés par l'avocat]");
        assertThat(prompt).contains("[Points procéduraux vérifiés — à reconsidérer si nécessaire]");
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
