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
    private final StrategicOptionService strategicOptionService = mock(StrategicOptionService.class);
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

    private final EnrichedAnalysisService service = new EnrichedAnalysisService(
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
        // F-194 SF-194-01 — stub par défaut pour collectForEnrichment (sinon NPE dans prepareEnrichedAnalysis)
        when(pieceManquanteStatusService.collectForEnrichment(any()))
                .thenReturn(PieceManquanteStatusService.EnrichmentSnapshot.empty());
        // F-195 SF-195-01 — stub par défaut risques (sinon NPE dans prepareEnrichedAnalysis)
        when(risqueStatusService.collectForEnrichment(any()))
                .thenReturn(RisqueStatusService.EnrichmentSnapshot.empty());
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

        // F-192 SF-192-01 — la matérialisation est appelée à la fin du run
        verify(retainedPisteAlignmentService, times(1)).materializeForAnalysis(any());
    }

    // F-192 SF-192-01 : si la matérialisation jette, le run réussit quand même (fail-open).
    @Test
    void consumeReAnalysis_materializationThrows_runStillCompletes() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setAnalysisResult("{\"faits\":[]}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[\"enrichi\"]}", "claude-sonnet-4-6", 100, 50));

        // F-192 — la matérialisation jette
        org.mockito.Mockito.doThrow(new RuntimeException("DB lock"))
                .when(retainedPisteAlignmentService).materializeForAnalysis(any());

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        // Le run termine en DONE quand même
        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(2)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
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

    // SF-155-04-00-BE-travail : la directive de préservation mentionne les 5 nouveaux champs IA
    @Test
    void systemPrompt_preservesNewPrefillIaTravailFields() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l,
                java.util.List.of("LICENCIEMENT_SANS_CAUSE_REELLE"));
        // La directive de préservation de travail_extracted_data doit lister les 5 nouveaux champs
        assertThat(prompt).contains("travail_extracted_data");
        assertThat(prompt).contains("motif_nullite_pressenti");
        assertThat(prompt).contains("origine_inaptitude_pressentie");
        assertThat(prompt).contains("avis_medecin_travail_date");
        assertThat(prompt).contains("reclassement_respecte_detected");
        assertThat(prompt).contains("heures_sup_mentionnees");
    }

    // SF-155-04-00-BE-immig-BE : la directive de préservation mentionne les 4 champs Annexe 13 BE
    @Test
    void systemPrompt_preservesAnnexe13BeFields() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "BELGIQUE", l,
                java.util.List.of("OQT_BELGIQUE"));
        // La directive de préservation de immigration_extracted_data doit lister les 4 nouveaux champs
        assertThat(prompt).contains("immigration_extracted_data");
        assertThat(prompt).contains("date_notification_annexe13");
        assertThat(prompt).contains("delai_depart_impose_jours");
        assertThat(prompt).contains("motif_oqt_code_be");
        assertThat(prompt).contains("transfert_imminent_detected");
    }

    // U-SF-96-06-01 : le prompt enrichi contient la règle de durcissement points_procedure
    // Mise à jour F-176 SF-176-01 : la règle de répartition redirige désormais vers pistes_strategiques
    @Test
    void systemPrompt_containsStrategicOptionsExclusionRuleForPointsProcedure() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l,
                java.util.List.of("OQTF_AVEC_DELAI"));
        assertThat(prompt).contains("SF-96-06");
        assertThat(prompt).contains("vérifications binaires factuelles");
        assertThat(prompt).contains("options stratégiques");
        assertThat(prompt).contains("opportunités futures");
        assertThat(prompt).contains("recommandations d'action");
        assertThat(prompt).contains("on VÉRIFIE dans points_procedure, on PROPOSE dans pistes_strategiques, on ALERTE dans risques");
    }

    // U-SF-IM-21-03-01 : les 18 codes IM21_* sont reconnus comme sourceKey valides pour F-IA-03 (prompt enrichi)
    @Test
    void systemPrompt_listsIm21CodesAsValidSourceKeys() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l,
                java.util.List.of("OQTF_AVEC_DELAI"));
        // La ligne "Codes F96 :" doit lister les 18 codes IM21_*
        int codesF96Idx = prompt.indexOf("Codes F96 :");
        assertThat(codesF96Idx).isGreaterThan(0);
        String codesF96Section = prompt.substring(codesF96Idx);
        java.util.List<String> codes = java.util.List.of(
                "IM21_REGULARITE_SEJOUR_FR", "IM21_DELAI_DEPOT_FR", "IM21_PIECE_IDENTITE_FR",
                "IM21_JUSTIF_DOMICILE_FR", "IM21_ETAT_CIVIL_FR", "IM21_PHOTO_FR",
                "IM21_TIMBRE_FISCAL_FR", "IM21_PIECES_MARIAGE_FR", "IM21_COMMUNAUTE_VIE_FR",
                "IM21_RESSOURCES_FR", "IM21_CONVENTION_ACCUEIL_FR",
                "IM21_REGULARITE_SEJOUR_BE", "IM21_PIECE_IDENTITE_BE",
                "IM21_PIECES_COHABITATION_BE", "IM21_RESSOURCES_BE",
                "IM21_LOGEMENT_BE", "IM21_ASSURANCE_BE", "IM21_EXTRAIT_CASIER_BE");
        for (String code : codes) {
            assertThat(codesF96Section).as("Code IM21 %s doit être dans la liste source_explanations enrichie", code).contains(code);
        }
    }

    // U-SF-IM-21-02-01 : le prompt enrichi liste les 18 codes binaires F-IM-21 (FR + BE)
    @Test
    void systemPrompt_containsIm21BinaryCriteriaCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l,
                java.util.List.of("OQTF_AVEC_DELAI"));
        assertThat(prompt).contains("F-IM-21");
        java.util.List<String> codes = java.util.List.of(
                "IM21_REGULARITE_SEJOUR_FR", "IM21_DELAI_DEPOT_FR", "IM21_PIECE_IDENTITE_FR",
                "IM21_JUSTIF_DOMICILE_FR", "IM21_ETAT_CIVIL_FR", "IM21_PHOTO_FR",
                "IM21_TIMBRE_FISCAL_FR", "IM21_PIECES_MARIAGE_FR", "IM21_COMMUNAUTE_VIE_FR",
                "IM21_RESSOURCES_FR", "IM21_CONVENTION_ACCUEIL_FR",
                "IM21_REGULARITE_SEJOUR_BE", "IM21_PIECE_IDENTITE_BE",
                "IM21_PIECES_COHABITATION_BE", "IM21_RESSOURCES_BE",
                "IM21_LOGEMENT_BE", "IM21_ASSURANCE_BE", "IM21_EXTRAIT_CASIER_BE");
        for (String code : codes) {
            assertThat(prompt).as("Code IM-21 %s doit être présent dans le prompt enrichi", code).contains(code);
        }
    }

    // U-SF-176-01-01 : le prompt enrichi contient le champ pistes_strategiques (F-176)
    @Test
    void systemPrompt_containsPistesStrategiquesField() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l,
                java.util.List.of("OQTF_AVEC_DELAI"));
        assertThat(prompt).contains("\"pistes_strategiques\": [...]");
        assertThat(prompt).contains("F-176");
        assertThat(prompt).contains("base_juridique");
        assertThat(prompt).contains("horizon_temporel");
        assertThat(prompt).contains("conditions");
    }

    // U-SF-176-01-02 : le prompt enrichi instruit Claude de NE PAS re-proposer les pistes DISCARDED
    @Test
    void systemPrompt_instructsClaudeNotToReProposeDiscarded() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l,
                java.util.List.of("OQTF_AVEC_DELAI"));
        assertThat(prompt).contains("Pistes stratégiques retenues à approfondir");
        assertThat(prompt).contains("Pistes stratégiques écartées — NE PAS re-proposer");
        assertThat(prompt).contains("ne remets PAS ces pistes");
    }

    // U-SF-176-01-03 : buildEnrichedPrompt injecte les sections de pistes stratégiques retenues + écartées
    @Test
    void buildEnrichedPrompt_injectsStrategicOptionsRetainedAndDiscardedSections() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(java.util.List.of());
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(caseFileId)).thenReturn(java.util.List.of());
        java.util.List<String> retained = java.util.List.of("Demande de Passeport talent — Chercheur (art. L.421-14 CESEDA)");
        java.util.List<String> discarded = java.util.List.of("Recours gracieux préfet (rejeté antérieurement)");
        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                java.util.List.of(), java.util.List.of(), java.util.List.of(), retained, discarded);
        assertThat(prompt).contains("[Pistes stratégiques retenues à approfondir]");
        assertThat(prompt).contains("Demande de Passeport talent — Chercheur");
        assertThat(prompt).contains("[Pistes stratégiques écartées — NE PAS re-proposer]");
        assertThat(prompt).contains("Recours gracieux préfet");
    }

    // U-SF-96-06-02 : non-régression — les codes énumérés F-DT-08/10, F-IM-05/06/07, F-FA-06/07, F-DT-09 restent inchangés
    @Test
    void systemPrompt_keepsExistingEnumeratedCriteriaCodesIntact() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = EnrichedAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l,
                java.util.List.of("LICENCIEMENT_SANS_CAUSE_REELLE"));
        // F-DT-08 codes (binaires)
        assertThat(prompt).contains("FR_CONVOCATION");
        assertThat(prompt).contains("FR_ENTRETIEN");
        assertThat(prompt).contains("BE_PREAVIS");
        // F-DT-10 codes (rupture conventionnelle)
        assertThat(prompt).contains("RC_CONSENTEMENT");
        assertThat(prompt).contains("RC_DELAI_RETRACTATION");
        // F-IM-05/06/07 codes (énumérés)
        assertThat(prompt).contains("IM05_MOTIF");
        assertThat(prompt).contains("IM06_RECOURS_TYPE");
        assertThat(prompt).contains("IM07_TITRE_TYPE");
        // F-FA-06/07 codes
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("FR_CHOIX_AVOCATS");
        // F-DT-09 énuméré
        assertThat(prompt).contains("DT09_TYPE_RUPTURE");
        // Règle générale d'usage du critere_code
        assertThat(prompt).contains("Pour tout point sans lien avec ces critères, \"critere_code\" et \"expected_value\" restent null");
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

    // F-190 SF-190-02 : streaming → partial_state alimenté par section, puis purgé au DONE
    @Test
    void consumeReAnalysis_streaming_setsPartialStateThenPurgesAtDone() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setAnalysisResult("{\"faits\":[]}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        // Streaming stub : invoque le callback avec 2 sections puis retourne le résultat final
        when(anthropicService.analyzeWithSystemCacheStreaming(any(), any(), anyInt(), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onDelta = inv.getArgument(3);
            onDelta.accept("{\"faits\": [{\"texte\": \"f1\"}], ");
            onDelta.accept("\"risques\": []}");
            return new AnthropicResult("{\"faits\":[{\"texte\":\"f1\"}],\"risques\":[]}",
                    "claude-sonnet-4-6", 100, 50);
        });

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeast(2)).save(captor.capture());
        boolean hasPartial = captor.getAllValues().stream()
                .anyMatch(a -> a.getPartialState() != null && !a.getPartialState().isBlank());
        assertThat(hasPartial).isTrue();
        CaseAnalysis last = captor.getValue();
        assertThat(last.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(last.getPartialState()).isNull();
    }

    // F-190 SF-190-02 : streaming retourne null → fallback synchrone analyzeWithSystemCache
    @Test
    void consumeReAnalysis_streamingReturnsNull_fallsBackToSynchronous() {
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

        // Streaming retourne null (mock par défaut) → le service doit retomber sur analyzeWithSystemCache
        when(anthropicService.analyzeWithSystemCacheStreaming(any(), any(), anyInt(), any())).thenReturn(null);
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[\"fallback\"]}", "claude-sonnet-4-6", 200, 100));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        verify(anthropicService).analyzeWithSystemCache(any(), any(), anyInt());
        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(captor.capture());
        CaseAnalysis last = captor.getValue();
        assertThat(last.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(last.getAnalysisResult()).contains("fallback");
    }

    // ====================================================================
    //  F-194 SF-194-01 — sections prompt enrichi pieces statuts avocat
    // ====================================================================

    @Test
    void buildEnrichedPrompt_withPiecesObtenues_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of("Contrat de travail original"), List.of(), List.of());

        assertThat(prompt).contains("[Pièces déjà obtenues — ne pas réclamer]");
        assertThat(prompt).contains("- Contrat de travail original");
    }

    @Test
    void buildEnrichedPrompt_withPiecesNonApplicables_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of("Convention BTP"), List.of());

        assertThat(prompt).contains("[Pièces non applicables au dossier — ne pas mentionner]");
        assertThat(prompt).contains("- Convention BTP");
    }

    @Test
    void buildEnrichedPrompt_withPiecesADemander_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of("Lettre de licenciement"));

        assertThat(prompt).contains("[Pièces à demander au client — pousser explicitement dans la nouvelle synthèse]");
        assertThat(prompt).contains("- Lettre de licenciement");
    }

    @Test
    void buildEnrichedPrompt_withAllThreePiecesStatuses_injectsThreeSections() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of("Contrat"), List.of("Convention BTP"),
                List.of("Lettre"));

        assertThat(prompt).contains("[Pièces déjà obtenues — ne pas réclamer]");
        assertThat(prompt).contains("[Pièces non applicables au dossier — ne pas mentionner]");
        assertThat(prompt).contains("[Pièces à demander au client — pousser explicitement dans la nouvelle synthèse]");
    }

    @Test
    void buildEnrichedPrompt_withoutPiecesStatuses_omitsAllThreeSections() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of());

        assertThat(prompt).doesNotContain("[Pièces déjà obtenues");
        assertThat(prompt).doesNotContain("[Pièces non applicables");
        assertThat(prompt).doesNotContain("[Pièces à demander");
    }

    @Test
    void consumeReAnalysis_callsPiecesMaterialization() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
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
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        // F-194 — le service de matérialisation pièces est appelé à la fin du run
        verify(pieceManquanteAlignmentService, times(1)).materializeForAnalysis(any());
    }

    @Test
    void consumeReAnalysis_piecesMaterializationThrows_runStillCompletes() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
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
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 100, 50));

        org.mockito.Mockito.doThrow(new RuntimeException("DB lock"))
                .when(pieceManquanteAlignmentService).materializeForAnalysis(any());

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        // Le run termine en DONE quand même
        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeast(1)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    // ====================================================================
    //  F-195 SF-195-01 — sections prompt enrichi risques statuts avocat
    // ====================================================================

    @Test
    void buildEnrichedPrompt_withRisquesValides_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of("Harcèlement moral subi"), List.of(), List.of());

        assertThat(prompt).contains("[Risques validés par votre avocat — à approfondir]");
        assertThat(prompt).contains("- Harcèlement moral subi");
    }

    @Test
    void buildEnrichedPrompt_withRisquesEcartesAvecRaison_injectsSectionWithReason() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(),
                List.of(new RisqueStatusService.EcarteEntry(
                        "Clause non-concurrence abusive", "Délai expiré")),
                List.of());

        assertThat(prompt).contains("[Risques écartés — NE PAS re-proposer]");
        assertThat(prompt).contains("- Clause non-concurrence abusive");
        assertThat(prompt).contains("(raison : Délai expiré)");
    }

    @Test
    void buildEnrichedPrompt_withRisquesACreuser_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of("Discrimination"));

        assertThat(prompt).contains("[Risques en cours d'instruction par votre avocat]");
        assertThat(prompt).contains("- Discrimination");
    }

    @Test
    void buildEnrichedPrompt_withoutRisquesStatuses_omitsAllRisquesSections() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of());

        assertThat(prompt).doesNotContain("[Risques validés");
        assertThat(prompt).doesNotContain("[Risques écartés");
        assertThat(prompt).doesNotContain("[Risques en cours d'instruction");
    }

    @Test
    void consumeReAnalysis_callsRisquesMaterialization() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
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
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        // F-195 — le service de matérialisation risques est appelé à la fin du run
        verify(risqueAlignmentService, times(1)).materializeForAnalysis(any());
    }

    @Test
    void consumeReAnalysis_risquesMaterializationThrows_runStillCompletes() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
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
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));

        org.mockito.Mockito.doThrow(new RuntimeException("DB lock"))
                .when(risqueAlignmentService).materializeForAnalysis(any());

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeast(1)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    // ====================================================================
    //  F-196 SF-196-01 — matérialisation questions complémentaires
    // ====================================================================

    @Test
    void consumeReAnalysis_callsAiQuestionsMaterialization() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
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
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        // F-196 — le service de matérialisation questions complémentaires est appelé à la fin du run
        verify(aiQuestionAlignmentService, times(1)).materializeForAnalysis(any());
    }

    @Test
    void consumeReAnalysis_aiQuestionsMaterializationThrows_runStillCompletes() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
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
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));

        org.mockito.Mockito.doThrow(new RuntimeException("DB lock"))
                .when(aiQuestionAlignmentService).materializeForAnalysis(any());

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        // Le run termine en DONE quand même (fail-open F-196)
        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeast(1)).save(captor.capture());
        assertThat(captor.getValue().getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
    }

    // ──────────────────────────── F-197 SF-197-01 — override avocat ─────────────────────────────

    // U-F197-01 : buildEnrichedPrompt sans override → pas de section [Type litige fixé par l'avocat]
    @Test
    void buildEnrichedPrompt_noOverride_omitsTypeLitigeSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                null);

        assertThat(prompt).doesNotContain("[Type litige fixé par l'avocat]");
    }

    // U-F197-02 : buildEnrichedPrompt avec override travail → section présente
    @Test
    void buildEnrichedPrompt_withTypeLitigeOverride_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        TypeLitigeOverrideService.OverrideSnapshot snapshot =
                new TypeLitigeOverrideService.OverrideSnapshot(
                        "LICENCIEMENT_ECONOMIQUE", null, "Avocat conviction");

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                snapshot);

        assertThat(prompt).contains("[Type litige fixé par l'avocat]");
        assertThat(prompt).contains("type_litige_detecte = LICENCIEMENT_ECONOMIQUE");
        assertThat(prompt).contains("Avocat conviction");
        assertThat(prompt).contains("CONSIGNE");
    }

    // U-F197-03 : buildEnrichedPrompt avec override immigration → section avec type_procedure
    @Test
    void buildEnrichedPrompt_withTypeProcedureOverride_injectsSection() {
        UUID caseFileId = UUID.randomUUID();
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        TypeLitigeOverrideService.OverrideSnapshot snapshot =
                new TypeLitigeOverrideService.OverrideSnapshot(
                        null, "OQTF_AVEC_DELAI", null);

        String prompt = service.buildEnrichedPrompt(caseFileId, "{}", null,
                List.of(), List.of(), List.of(),
                List.of(), List.of(),
                List.of(), List.of(), List.of(),
                List.of(), List.of(), List.of(),
                snapshot);

        assertThat(prompt).contains("[Type litige fixé par l'avocat]");
        assertThat(prompt).contains("type_procedure_detectee = OQTF_AVEC_DELAI");
    }

    // U-F197-04 : finalize → cloneOverrideFromPrevious appelé même si pas d'override IA
    @Test
    void consumeReAnalysis_callsCloneOverrideOnFinalization() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setAnalysisResult("{\"faits\":[\"f1\"]}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
        previousAnalysis.setTypeLitigeAvocatOverride("LICENCIEMENT_ECONOMIQUE");

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"faits\":[]}", "claude-sonnet-4-6", 100, 50));

        service.consumeReAnalysis(new ReAnalysisMessage(caseFileId));

        // Vérifie que cloneOverrideFromPrevious a bien été appelé sur la nouvelle analyse
        verify(typeLitigeOverrideService, times(1))
                .cloneOverrideFromPrevious(any(), any(CaseAnalysis.class));
    }

    // U-F197-05 : prompt enrichi inclut la section override quand previousAnalysis l'a
    @Test
    void prepareEnrichedAnalysis_previousHasOverride_promptIncludesSection() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        fr.ailegalcase.workspace.Workspace ws = new fr.ailegalcase.workspace.Workspace();
        ws.setLegalDomain("DROIT_DU_TRAVAIL");
        ws.setCountry("FRANCE");
        caseFile.setWorkspace(ws);

        CaseAnalysis previousAnalysis = new CaseAnalysis();
        previousAnalysis.setId(UUID.randomUUID());
        previousAnalysis.setAnalysisResult("{}");
        previousAnalysis.setAnalysisStatus(AnalysisStatus.DONE);
        previousAnalysis.setTypeLitigeAvocatOverride("HARCELEMENT_MORAL");
        previousAnalysis.setTypeOverrideRaison("avocat conviction");

        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.ENRICHED_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE))
                .thenReturn(Optional.of(previousAnalysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(caseFileId)).thenReturn(List.of());

        EnrichedAnalysisService.PreparedEnrichedAnalysis prepared =
                service.prepareEnrichedAnalysis(new ReAnalysisMessage(caseFileId));

        assertThat(prepared).isNotNull();
        assertThat(prepared.prompt()).contains("[Type litige fixé par l'avocat]");
        assertThat(prepared.prompt()).contains("HARCELEMENT_MORAL");
        assertThat(prepared.prompt()).contains("avocat conviction");
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
