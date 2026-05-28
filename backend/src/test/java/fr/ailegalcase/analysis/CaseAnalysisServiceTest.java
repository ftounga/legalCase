package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseDeadlineService;
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
    private final fr.ailegalcase.document.DocumentExtractionRepository documentExtractionRepository =
            mock(fr.ailegalcase.document.DocumentExtractionRepository.class);
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
    private final PiecesPromptContext piecesPromptContext = mock(PiecesPromptContext.class);
    private final StrategicOptionService strategicOptionService = mock(StrategicOptionService.class);
    private final JurisprudenceVerificationService jurisprudenceVerificationService =
            mock(JurisprudenceVerificationService.class);

    private final CaseAnalysisService service = new CaseAnalysisService(
            documentAnalysisRepository, documentExtractionRepository, caseAnalysisRepository, caseFileRepository,
            anthropicService, analysisJobRepository, rabbitTemplate, usageEventService, eventPublisher,
            analysisDocumentSnapshotService, analysisLimitsProperties, procedureCheckService, strategicOptionService, caseDeadlineService,
            sourceExplanationGenerator, sourceExplanationService, piecesPromptContext, jurisprudenceVerificationService);

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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));
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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
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

    // F-185 SF-185-01 : streaming → partial_state alimenté par section, puis purgé au DONE
    @Test
    void consumeCaseAnalysis_streaming_setsPartialStateThenPurgesAtDone() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        DocumentAnalysis da = documentAnalysis("{\"faits\":[]}", Instant.now());

        when(documentAnalysisRepository.findByDocumentCaseFileIdAndAnalysisStatus(caseFileId, AnalysisStatus.DONE))
                .thenReturn(List.of(da));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseAnalysisRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Streaming stub : invoque le callback avec 2 sections puis retourne le résultat final
        when(anthropicService.analyzeWithSystemCacheStreaming(any(AiCallContext.class), any(), any(), anyInt(), any())).thenAnswer(inv -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> onDelta = inv.getArgument(4);
            onDelta.accept("{\"faits\": [{\"texte\": \"f1\"}], ");
            onDelta.accept("\"risques\": []}");
            return new AnthropicResult("{\"faits\":[{\"texte\":\"f1\"}],\"risques\":[]}",
                    "claude-sonnet-4-6", 100, 50);
        });

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeast(2)).save(captor.capture());
        // Au moins une sauvegarde a positionné partial_state pendant le stream
        boolean hasPartial = captor.getAllValues().stream()
                .anyMatch(a -> a.getPartialState() != null && !a.getPartialState().isBlank());
        assertThat(hasPartial).isTrue();
        // La dernière sauvegarde (finalize DONE) a purgé partial_state
        CaseAnalysis last = captor.getValue();
        assertThat(last.getAnalysisStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(last.getPartialState()).isNull();
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
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).contains("timeline");
        assertThat(prompt).contains("date");
        assertThat(prompt).contains("evenement");
    }

    // U-05 : le system prompt contient le champ delais_detectes
    @Test
    void systemPrompt_containsDelaisDetectesField() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).contains("delais_detectes");
        assertThat(prompt).contains("date_detectee");
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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeWithSystemCache(any(AiCallContext.class), any(), promptCaptor.capture(), anyInt());
        String prompt = promptCaptor.getValue();
        assertThat(prompt.indexOf("document-0")).isLessThan(prompt.indexOf("document-1"));
        assertThat(prompt).contains("{\"faits\":[\"A\"]}");
        assertThat(prompt.indexOf("{\"faits\":[\"A\"]}")).isLessThan(prompt.indexOf("{\"faits\":[\"B\"]}"));
    }

    // U-07 : le system prompt contient les contraintes de longueur SF-28-01
    // Mise à jour F-161 SF-161-01 : reformulation "jusqu'à N… pas de minimum"
    @Test
    void systemPrompt_containsLengthConstraints() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(80); l.setPointsJuridiques(80); l.setRisques(40);
        l.setQuestionsOuvertes(40); l.setTimeline(60);
        l.setPiecesManquantes(40); l.setPointsProcedure(30); l.setPistesStrategiques(20);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).contains("produis jusqu'à 60 entrées timeline");
        assertThat(prompt).contains("80 faits");
        assertThat(prompt).contains("80 points_juridiques");
        assertThat(prompt).contains("40 risques");
        assertThat(prompt).contains("40 questions_ouvertes");
        assertThat(prompt).contains("40 pièces manquantes");
        assertThat(prompt).contains("30 points procédure");
        assertThat(prompt).contains("20 pistes stratégiques");
        assertThat(prompt).contains("Pas de minimum");
        assertThat(prompt).contains("sans rembourrer");
    }

    // U-SF-96-06-01 : le system prompt contient la règle de durcissement points_procedure
    // Mise à jour F-176 SF-176-01 : la règle de répartition redirige désormais vers pistes_strategiques
    @Test
    void systemPrompt_containsStrategicOptionsExclusionRuleForPointsProcedure() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).contains("SF-96-06");
        assertThat(prompt).contains("vérifications binaires factuelles");
        assertThat(prompt).contains("options stratégiques");
        assertThat(prompt).contains("opportunités futures");
        assertThat(prompt).contains("recommandations d'action");
        assertThat(prompt).contains("on VÉRIFIE dans points_procedure, on PROPOSE dans pistes_strategiques, on ALERTE dans risques");
    }

    // U-SF-IM-21-03-01 : les 18 codes IM21_* sont reconnus comme sourceKey valides pour F-IA-03
    @Test
    void systemPrompt_listsIm21CodesAsValidSourceKeys() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l);
        // La ligne "Codes F96 additionnels possibles" doit lister les 18 codes IM21_*
        int sourceExplanationsIdx = prompt.indexOf("Codes F96 additionnels possibles");
        assertThat(sourceExplanationsIdx).isGreaterThan(0);
        String sourceExplanationsSection = prompt.substring(sourceExplanationsIdx);
        java.util.List<String> codes = java.util.List.of(
                "IM21_REGULARITE_SEJOUR_FR", "IM21_DELAI_DEPOT_FR", "IM21_PIECE_IDENTITE_FR",
                "IM21_JUSTIF_DOMICILE_FR", "IM21_ETAT_CIVIL_FR", "IM21_PHOTO_FR",
                "IM21_TIMBRE_FISCAL_FR", "IM21_PIECES_MARIAGE_FR", "IM21_COMMUNAUTE_VIE_FR",
                "IM21_RESSOURCES_FR", "IM21_CONVENTION_ACCUEIL_FR",
                "IM21_REGULARITE_SEJOUR_BE", "IM21_PIECE_IDENTITE_BE",
                "IM21_PIECES_COHABITATION_BE", "IM21_RESSOURCES_BE",
                "IM21_LOGEMENT_BE", "IM21_ASSURANCE_BE", "IM21_EXTRAIT_CASIER_BE");
        for (String code : codes) {
            assertThat(sourceExplanationsSection).as("Code IM21 %s doit être dans la liste source_explanations", code).contains(code);
        }
    }

    // U-SF-IM-21-02-01 : le system prompt liste les 18 codes binaires F-IM-21 (FR + BE)
    @Test
    void systemPrompt_containsIm21BinaryCriteriaCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l);
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
            assertThat(prompt).as("Code IM-21 %s doit être présent dans le prompt", code).contains(code);
        }
    }

    // U-SF-176-01-01 : le system prompt contient le champ pistes_strategiques (F-176)
    @Test
    void systemPrompt_containsPistesStrategiquesField() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).contains("\"pistes_strategiques\": [...]");
        assertThat(prompt).contains("F-176");
        assertThat(prompt).contains("base_juridique");
        assertThat(prompt).contains("horizon_temporel");
        assertThat(prompt).contains("conditions");
        assertThat(prompt).contains("Passeport talent — Chercheur");
    }

    // U-SF-96-06-02 : non-régression — les codes énumérés F-DT-08 / F-IM-05/06/07 / F-FA-06/07 / F-DT-09 restent inchangés
    @Test
    void systemPrompt_keepsExistingEnumeratedCriteriaCodesIntact() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        // F-DT-08 codes (binaires)
        assertThat(prompt).contains("FR_CONVOCATION");
        assertThat(prompt).contains("FR_ENTRETIEN");
        assertThat(prompt).contains("BE_PREAVIS");
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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{}", "claude-sonnet-4-6", 10, 5));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeCaseAnalysis(new CaseAnalysisMessage(caseFileId));

        ArgumentCaptor<CaseAnalysis> captor = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getAllValues().get(0).getVersion()).isEqualTo(3);
    }

    // SF-250-02 — Remédiation critereCode lot Travail FR validité/procédure

    // U-SF-250-02-01 : le prompt système travail-FR contient les 3 codes DT36_*
    @Test
    void systemPrompt_containsDt36CriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).as("DT36_DATE_ENTRETIEN doit être dans le prompt points_procedure").contains("DT36_DATE_ENTRETIEN");
        assertThat(prompt).as("DT36_MOTIVATION doit être dans le prompt points_procedure").contains("DT36_MOTIVATION");
        assertThat(prompt).as("DT36_ENTRETIEN_TENU doit être dans le prompt points_procedure").contains("DT36_ENTRETIEN_TENU");
        // Référence outil dans le prompt
        assertThat(prompt).as("F-DT-36 doit être cité dans le prompt").contains("F-DT-36");
    }

    // U-SF-250-02-02 : le prompt système travail-FR contient HLN_, DT13_, PSE_, PROTECTION_RP_
    @Test
    void systemPrompt_containsHlnAndDt13AndPseAndProtectionRpCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).as("HLN_MOTIF_NULLITE doit être dans le prompt").contains("HLN_MOTIF_NULLITE");
        assertThat(prompt).as("DT13_MOTIF_ECONOMIQUE doit être dans le prompt").contains("DT13_MOTIF_ECONOMIQUE");
        assertThat(prompt).as("DT13_DATE_NOTIFICATION doit être dans le prompt").contains("DT13_DATE_NOTIFICATION");
        assertThat(prompt).as("PSE_DATE_PROJET doit être dans le prompt").contains("PSE_DATE_PROJET");
        assertThat(prompt).as("PROTECTION_RP_MOTIF doit être dans le prompt").contains("PROTECTION_RP_MOTIF");
    }

    // U-SF-250-02-03 : les nouveaux codes sont binaires — sémantique null expected_value documentée
    @Test
    void systemPrompt_travailFrNewCodesHaveNullExpectedValue() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        // Vérifier que la sémantique binaire ("expected_value" reste null) est documentée pour les nouveaux codes
        // On cherche que le bloc DT36 mentionne "expected_value" doit rester null
        int dt36Idx = prompt.indexOf("DT36_DATE_ENTRETIEN");
        assertThat(dt36Idx).as("DT36_DATE_ENTRETIEN doit être présent").isGreaterThan(0);
        // Dans le bloc DT36, on doit voir "expected_value" et "null"
        String dt36Block = prompt.substring(dt36Idx, Math.min(dt36Idx + 500, prompt.length()));
        assertThat(dt36Block).as("Bloc DT36 doit mentionner expected_value null").contains("expected_value");
        // Non-régression : les codes existants F-DT-08 ne sont pas altérés
        assertThat(prompt).contains("FR_CONVOCATION");
        assertThat(prompt).contains("FR_ENTRETIEN");
        assertThat(prompt).contains("DT09_TYPE_RUPTURE");
    }

    // SF-250-03 — Remédiation critereCode lot Travail FR rupture/requalifications/inaptitude/AT-MP

    // U-SF-250-03-01 : le prompt système travail-FR contient les 6 codes RC_*
    @Test
    void systemPrompt_containsRcCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).as("RC_CONSENTEMENT doit être dans le prompt points_procedure").contains("RC_CONSENTEMENT");
        assertThat(prompt).as("RC_DELAI_RETRACTATION doit être dans le prompt points_procedure").contains("RC_DELAI_RETRACTATION");
        assertThat(prompt).as("RC_HOMOLOGATION doit être dans le prompt points_procedure").contains("RC_HOMOLOGATION");
        assertThat(prompt).as("RC_ASSISTANCE doit être dans le prompt points_procedure").contains("RC_ASSISTANCE");
        assertThat(prompt).as("RC_INDEMNITE doit être dans le prompt points_procedure").contains("RC_INDEMNITE");
        assertThat(prompt).as("RC_ENTRETIENS doit être dans le prompt points_procedure").contains("RC_ENTRETIENS");
        assertThat(prompt).as("F-DT-10 doit être cité dans le prompt").contains("F-DT-10");
    }

    // U-SF-250-03-02 : le prompt système travail-FR contient DT22_, DT23_, DT24_, DT31_, RCI_
    @Test
    void systemPrompt_containsDt22Dt23Dt24Dt31RciCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).as("DT22_SALAIRE doit être dans le prompt").contains("DT22_SALAIRE");
        assertThat(prompt).as("DT23_SALAIRE doit être dans le prompt").contains("DT23_SALAIRE");
        assertThat(prompt).as("DT24_SALAIRE doit être dans le prompt").contains("DT24_SALAIRE");
        assertThat(prompt).as("DT31_SALAIRE_MENSUEL doit être dans le prompt").contains("DT31_SALAIRE_MENSUEL");
        assertThat(prompt).as("DT31_ANCIENNETE doit être dans le prompt").contains("DT31_ANCIENNETE");
        assertThat(prompt).as("RCI_SALAIRE doit être dans le prompt").contains("RCI_SALAIRE");
        assertThat(prompt).as("RCI_ANCIENNETE doit être dans le prompt").contains("RCI_ANCIENNETE");
        // Non-régression codes SF-250-02
        assertThat(prompt).contains("DT36_DATE_ENTRETIEN");
        assertThat(prompt).contains("HLN_MOTIF_NULLITE");
    }

    // U-SF-250-03-03 : le prompt système travail-FR contient INAPT_ et AT_MP_
    @Test
    void systemPrompt_containsInapt_andAtMpCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).as("INAPT_ORIGINE doit être dans le prompt").contains("INAPT_ORIGINE");
        assertThat(prompt).as("INAPT_RECLASSEMENT doit être dans le prompt").contains("INAPT_RECLASSEMENT");
        assertThat(prompt).as("AT_MP_DATE_ACCIDENT doit être dans le prompt").contains("AT_MP_DATE_ACCIDENT");
        assertThat(prompt).as("F-DT-15 doit être cité dans le prompt").contains("F-DT-15");
        assertThat(prompt).as("F-DT-33 doit être cité dans le prompt").contains("F-DT-33");
        // Sémantique binaire : expected_value null documentée
        int inapt_idx = prompt.indexOf("INAPT_ORIGINE");
        assertThat(inapt_idx).isGreaterThan(0);
        String inaptBlock = prompt.substring(inapt_idx, Math.min(inapt_idx + 400, prompt.length()));
        assertThat(inaptBlock).as("Bloc INAPT doit mentionner expected_value null").contains("expected_value");
    }

    // SF-250-07 — Remédiation critereCode lot Immigration BE

    // U-SF-250-07-01 : le prompt système immigration-BE contient le code IM08_MOTIF_OQT_BE annoté BELGIQUE UNIQUEMENT
    @Test
    void systemPrompt_containsIm08OqtBeCritereCode() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "BELGIQUE", l);
        assertThat(prompt).as("IM08_MOTIF_OQT_BE doit être dans le prompt").contains("IM08_MOTIF_OQT_BE");
        assertThat(prompt).as("F-IM-08 doit être cité dans le prompt (BE)").contains("F-IM-08");
        assertThat(prompt).as("Annotation BELGIQUE UNIQUEMENT requise pour IM08_MOTIF_OQT_BE").contains("BELGIQUE UNIQUEMENT");
        // Non-régression — codes FR toujours présents
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
        assertThat(prompt).contains("IM11_TITRE_ACTUEL");
    }

    // U-SF-250-07-02 : le prompt système immigration contient BE_9TER_* et IM17_VOIE_REGIME_ALGERIEN
    @Test
    void systemPrompt_containsBe9terAndIm17CriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "BELGIQUE", l);
        assertThat(prompt).as("BE_9TER_MALADIE_GRAVE doit être dans le prompt").contains("BE_9TER_MALADIE_GRAVE");
        assertThat(prompt).as("BE_9TER_SOINS_BE doit être dans le prompt").contains("BE_9TER_SOINS_BE");
        assertThat(prompt).as("BE_9TER_SOINS_INACCESSIBLES doit être dans le prompt").contains("BE_9TER_SOINS_INACCESSIBLES");
        assertThat(prompt).as("BE_9TER_MENACE_ORDRE_PUBLIC doit être dans le prompt").contains("BE_9TER_MENACE_ORDRE_PUBLIC");
        assertThat(prompt).as("IM17_VOIE_REGIME_ALGERIEN doit être dans le prompt").contains("IM17_VOIE_REGIME_ALGERIEN");
        assertThat(prompt).as("F-IM-14 doit être cité dans le prompt").contains("F-IM-14");
        assertThat(prompt).as("F-IM-17 doit être cité dans le prompt").contains("F-IM-17");
        // Non-régression codes SF-250-05, SF-250-06
        assertThat(prompt).contains("IM24_DATE_ORDONNANCE_PROTECTION");
        assertThat(prompt).contains("IM09H_MOTIF_HUMANITAIRE");
    }

    // SF-250-06 — Remédiation critereCode lot Immigration FR — titres / asile / éloignement

    // U-SF-250-06-01 : le prompt système immigration-FR contient les codes IM11_, IM12_, IM13_
    @Test
    void systemPrompt_containsIm11Im12Im13CriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).as("IM11_TITRE_ACTUEL doit être dans le prompt").contains("IM11_TITRE_ACTUEL");
        assertThat(prompt).as("IM12_DISPOSITIF_ASILE doit être dans le prompt").contains("IM12_DISPOSITIF_ASILE");
        assertThat(prompt).as("IM13_VOIE_NATURALISATION doit être dans le prompt").contains("IM13_VOIE_NATURALISATION");
        assertThat(prompt).as("F-IM-11 doit être cité dans le prompt").contains("F-IM-11");
        assertThat(prompt).as("F-IM-12 doit être cité dans le prompt").contains("F-IM-12");
        assertThat(prompt).as("F-IM-13 doit être cité dans le prompt").contains("F-IM-13");
        // Non-régression codes SF-250-05
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
        assertThat(prompt).contains("IM09H_MOTIF_HUMANITAIRE");
    }

    // U-SF-250-06-02 : le prompt système immigration-FR contient les codes IM19_* (mineurs)
    @Test
    void systemPrompt_containsIm19MineursCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).as("IM19_DATE_NAISSANCE doit être dans le prompt").contains("IM19_DATE_NAISSANCE");
        assertThat(prompt).as("IM19_DATE_ENTREE doit être dans le prompt").contains("IM19_DATE_ENTREE");
        assertThat(prompt).as("F-IM-19 doit être cité dans le prompt").contains("F-IM-19");
    }

    // U-SF-250-06-03 : le prompt système immigration-FR contient IM20_* et IM24_*
    @Test
    void systemPrompt_containsIm20EloignementAndIm24ViolencesCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).as("IM20_DISPOSITIF_ELOIGNEMENT doit être dans le prompt").contains("IM20_DISPOSITIF_ELOIGNEMENT");
        assertThat(prompt).as("IM20_MOTIF_MENACE doit être dans le prompt").contains("IM20_MOTIF_MENACE");
        assertThat(prompt).as("IM24_DATE_ORDONNANCE_PROTECTION doit être dans le prompt").contains("IM24_DATE_ORDONNANCE_PROTECTION");
        assertThat(prompt).as("F-IM-20 doit être cité dans le prompt").contains("F-IM-20");
        assertThat(prompt).as("F-IM-24 doit être cité dans le prompt").contains("F-IM-24");
        // Annotation FRANCE UNIQUEMENT présente
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour IM20/IM24").contains("FRANCE UNIQUEMENT");
        // Non-régression codes SF-250-05 + SF-250-04
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
        assertThat(prompt).contains("CREDIT_TEMPS_ANCIENNETE");
    }

    // SF-250-05 — Remédiation critereCode lot Immigration FR — OQTF / AES

    // U-SF-250-05-01 : le prompt système immigration-FR contient les codes IM08_* (OQTF + référés)
    @Test
    void systemPrompt_containsIm08OqtfCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).as("IM08_MOTIF_OQTF doit être dans le prompt").contains("IM08_MOTIF_OQTF");
        assertThat(prompt).as("IM08_RECOURS_FORME doit être dans le prompt").contains("IM08_RECOURS_FORME");
        assertThat(prompt).as("IM08RA_DECISION_CONTESTEE doit être dans le prompt").contains("IM08RA_DECISION_CONTESTEE");
        assertThat(prompt).as("F-IM-08 doit être cité dans le prompt").contains("F-IM-08");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour IM08").contains("FRANCE UNIQUEMENT");
        // Non-régression
        assertThat(prompt).contains("IM05_MOTIF");
        assertThat(prompt).contains("IM06_RECOURS_TYPE");
        assertThat(prompt).contains("IM21_REGULARITE_SEJOUR_FR");
    }

    // U-SF-250-05-02 : le prompt système immigration-FR contient les codes IM09_* AES métiers tension
    @Test
    void systemPrompt_containsIm09AesMetiersTensionCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).as("IM09_DATE_ENTREE_FRANCE doit être dans le prompt").contains("IM09_DATE_ENTREE_FRANCE");
        assertThat(prompt).as("IM09_MOIS_ACTIVITE doit être dans le prompt").contains("IM09_MOIS_ACTIVITE");
        assertThat(prompt).as("F-IM-09 doit être cité dans le prompt").contains("F-IM-09");
    }

    // U-SF-250-05-03 : le prompt système immigration-FR contient les codes IM09_ETU_* et IM09H_*
    @Test
    void systemPrompt_containsIm09EtudiantAndHumanitaireCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).as("IM09_ETU_DATE_ENTREE_FRANCE doit être dans le prompt").contains("IM09_ETU_DATE_ENTREE_FRANCE");
        assertThat(prompt).as("IM09_ETU_DUREE_PRESENCE doit être dans le prompt").contains("IM09_ETU_DUREE_PRESENCE");
        assertThat(prompt).as("IM09_ETU_DATE_DEPOT_DEMANDE doit être dans le prompt").contains("IM09_ETU_DATE_DEPOT_DEMANDE");
        assertThat(prompt).as("IM09H_DATE_ENTREE_FRANCE doit être dans le prompt").contains("IM09H_DATE_ENTREE_FRANCE");
        assertThat(prompt).as("IM09H_MOTIF_HUMANITAIRE doit être dans le prompt").contains("IM09H_MOTIF_HUMANITAIRE");
        // Non-régression codes SF-250-04
        assertThat(prompt).contains("CREDIT_TEMPS_ANCIENNETE");
        assertThat(prompt).contains("TRAVAIL_PROCEDURE_TYPE");
    }

    // SF-250-04 — Remédiation critereCode lot Travail BE + F-136

    // U-SF-250-04-01 : le prompt système travail contient les codes CREDIT_TEMPS_* annotés BELGIQUE UNIQUEMENT
    @Test
    void systemPrompt_containsCreditTempsCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "BELGIQUE", l);
        assertThat(prompt).as("CREDIT_TEMPS_ANCIENNETE doit être dans le prompt").contains("CREDIT_TEMPS_ANCIENNETE");
        assertThat(prompt).as("CREDIT_TEMPS_AGE doit être dans le prompt").contains("CREDIT_TEMPS_AGE");
        assertThat(prompt).as("F-DT-29 doit être cité dans le prompt").contains("F-DT-29");
        assertThat(prompt).as("Annotation BELGIQUE UNIQUEMENT requise pour CREDIT_TEMPS").contains("BELGIQUE UNIQUEMENT");
        // Non-régression codes SF-250-02 et SF-250-03
        assertThat(prompt).contains("DT36_DATE_ENTRETIEN");
        assertThat(prompt).contains("AT_MP_DATE_ACCIDENT");
    }

    // U-SF-250-04-02 : le prompt système travail contient TRAVAIL_PROCEDURE_TYPE
    @Test
    void systemPrompt_containsTravailProcedureTypeCritereCode() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE", l);
        assertThat(prompt).as("TRAVAIL_PROCEDURE_TYPE doit être dans le prompt").contains("TRAVAIL_PROCEDURE_TYPE");
        assertThat(prompt).as("F-136 doit être cité dans le prompt").contains("F-136");
        // Sémantique binaire : expected_value null
        int idx = prompt.indexOf("TRAVAIL_PROCEDURE_TYPE");
        assertThat(idx).isGreaterThan(0);
        String block = prompt.substring(idx, Math.min(idx + 300, prompt.length()));
        assertThat(block).as("Bloc TRAVAIL_PROCEDURE_TYPE doit mentionner expected_value null").contains("expected_value");
    }

    // SF-250-09 — Remédiation critereCode lot Famille FR — successions / libéralités

    // U-SF-250-09-01 : le prompt système famille-FR contient TESTAMENT_* et DONATION_*
    @Test
    void systemPrompt_containsTestamentDonationCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE", l);
        assertThat(prompt).as("TESTAMENT_FORME doit être dans le prompt").contains("TESTAMENT_FORME");
        assertThat(prompt).as("TESTAMENT_SAINE_ESPRIT doit être dans le prompt").contains("TESTAMENT_SAINE_ESPRIT");
        assertThat(prompt).as("TESTAMENT_QUOTITE doit être dans le prompt").contains("TESTAMENT_QUOTITE");
        assertThat(prompt).as("DONATION_FORME doit être dans le prompt").contains("DONATION_FORME");
        assertThat(prompt).as("DONATION_SAINE_ESPRIT doit être dans le prompt").contains("DONATION_SAINE_ESPRIT");
        assertThat(prompt).as("DONATION_QUOTITE doit être dans le prompt").contains("DONATION_QUOTITE");
        assertThat(prompt).as("F-FA-24 doit être cité dans le prompt").contains("F-FA-24");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour codes successions").contains("FRANCE UNIQUEMENT");
        // Non-régression codes SF-250-08
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("FA22_OCCUPATION");
    }

    // U-SF-250-09-02 : le prompt système famille-FR contient PARTAGE_*, INDIVISION_*, DEVOLUTION_LEGALE_*
    @Test
    void systemPrompt_containsPartageIndivisionDevolutionCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE", l);
        assertThat(prompt).as("PARTAGE_MODE doit être dans le prompt").contains("PARTAGE_MODE");
        assertThat(prompt).as("PARTAGE_CONSENTEMENTS doit être dans le prompt").contains("PARTAGE_CONSENTEMENTS");
        assertThat(prompt).as("PARTAGE_PRESENCE_IMMEUBLES doit être dans le prompt").contains("PARTAGE_PRESENCE_IMMEUBLES");
        assertThat(prompt).as("INDIVISION_DATE_OUVERTURE doit être dans le prompt").contains("INDIVISION_DATE_OUVERTURE");
        assertThat(prompt).as("INDIVISION_TYPE doit être dans le prompt").contains("INDIVISION_TYPE");
        assertThat(prompt).as("DEVOLUTION_LEGALE_CONJOINT doit être dans le prompt").contains("DEVOLUTION_LEGALE_CONJOINT");
        assertThat(prompt).as("DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS doit être dans le prompt").contains("DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS");
        // Non-régression
        assertThat(prompt).contains("TESTAMENT_FORME");
        assertThat(prompt).contains("FA22_DATE_ORIGINE");
    }

    // U-SF-250-09-03 : le prompt système famille-FR contient FA25_*, FA26_*, PMA_GPA_DISPOSITIF, FA05_*
    @Test
    void systemPrompt_containsFa25Fa26PmaGpaFa05CriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE", l);
        assertThat(prompt).as("FA25_DATE_CERTIFICAT doit être dans le prompt").contains("FA25_DATE_CERTIFICAT");
        assertThat(prompt).as("FA25_ALT_MENTALES doit être dans le prompt").contains("FA25_ALT_MENTALES");
        assertThat(prompt).as("FA25_CONSENTEMENT doit être dans le prompt").contains("FA25_CONSENTEMENT");
        assertThat(prompt).as("FA25_DEMANDEUR_FAMILIAL doit être dans le prompt").contains("FA25_DEMANDEUR_FAMILIAL");
        assertThat(prompt).as("FA26_TYPE_CHANGEMENT doit être dans le prompt").contains("FA26_TYPE_CHANGEMENT");
        assertThat(prompt).as("FA26_MOTIF_INVOQUE doit être dans le prompt").contains("FA26_MOTIF_INVOQUE");
        assertThat(prompt).as("FA26_DATE_NAISSANCE doit être dans le prompt").contains("FA26_DATE_NAISSANCE");
        assertThat(prompt).as("FA26_MAJEUR_DEMANDEUR doit être dans le prompt").contains("FA26_MAJEUR_DEMANDEUR");
        assertThat(prompt).as("FA26_CONSENTEMENT_PARENTAL doit être dans le prompt").contains("FA26_CONSENTEMENT_PARENTAL");
        assertThat(prompt).as("PMA_GPA_DISPOSITIF doit être dans le prompt").contains("PMA_GPA_DISPOSITIF");
        assertThat(prompt).as("FA05_VALEUR_VENALE doit être dans le prompt").contains("FA05_VALEUR_VENALE");
        assertThat(prompt).as("FA05_CAPITAL_RESTANT doit être dans le prompt").contains("FA05_CAPITAL_RESTANT");
        assertThat(prompt).as("F-FA-25 doit être cité dans le prompt").contains("F-FA-25");
        assertThat(prompt).as("F-FA-26 doit être cité dans le prompt").contains("F-FA-26");
        assertThat(prompt).as("F-FA-27 doit être cité dans le prompt").contains("F-FA-27");
        assertThat(prompt).as("F-FA-05 doit être cité dans le prompt").contains("F-FA-05");
        // Non-régression
        assertThat(prompt).contains("TESTAMENT_FORME");
        assertThat(prompt).contains("PARTAGE_MODE");
    }

    // SF-250-08 — Remédiation critereCode lot Famille FR — divorce / union

    // U-SF-250-08-01 : le prompt système famille-FR contient DA_DUREE_MARIAGE, FA09_*, FA12_*, FA13_, FA14_*
    @Test
    void systemPrompt_containsDaFa09Fa12Fa13Fa14CriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE", l);
        assertThat(prompt).as("DA_DUREE_MARIAGE doit être dans le prompt").contains("DA_DUREE_MARIAGE");
        assertThat(prompt).as("FA09_DUREE_MARIAGE doit être dans le prompt").contains("FA09_DUREE_MARIAGE");
        assertThat(prompt).as("FA09_DATE_DEPOT_ASSIGNATION doit être dans le prompt").contains("FA09_DATE_DEPOT_ASSIGNATION");
        assertThat(prompt).as("FA09_FAUTES_INVOQUEES doit être dans le prompt").contains("FA09_FAUTES_INVOQUEES");
        assertThat(prompt).as("FA12_DATE_AUDIENCE doit être dans le prompt").contains("FA12_DATE_AUDIENCE");
        assertThat(prompt).as("FA12_VIOLENCES doit être dans le prompt").contains("FA12_VIOLENCES");
        assertThat(prompt).as("FA13_NB_ENFANTS doit être dans le prompt").contains("FA13_NB_ENFANTS");
        assertThat(prompt).as("FA14_DATE_REQUETE doit être dans le prompt").contains("FA14_DATE_REQUETE");
        assertThat(prompt).as("FA14_VIOLENCES_ALLEGUEES doit être dans le prompt").contains("FA14_VIOLENCES_ALLEGUEES");
        assertThat(prompt).as("FA14_LOGEMENT_COMMUN doit être dans le prompt").contains("FA14_LOGEMENT_COMMUN");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour codes FA famille").contains("FRANCE UNIQUEMENT");
        // Non-régression codes antérieurs
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("FR_CHOIX_AVOCATS");
    }

    // U-SF-250-08-02 : le prompt système famille-FR contient FA15_, COMMUNAUTE_UNIVERSELLE_*, PARTAGE_JUDICIAIRE_*
    @Test
    void systemPrompt_containsFa15CommunauteUniversellePartageJudiciaireCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE", l);
        assertThat(prompt).as("FA15_REGIME_MATRIMONIAL doit être dans le prompt").contains("FA15_REGIME_MATRIMONIAL");
        assertThat(prompt).as("COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE doit être dans le prompt").contains("COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE");
        assertThat(prompt).as("COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS doit être dans le prompt").contains("COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS");
        assertThat(prompt).as("PARTAGE_JUDICIAIRE_PV doit être dans le prompt").contains("PARTAGE_JUDICIAIRE_PV");
        assertThat(prompt).as("PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE doit être dans le prompt").contains("PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE");
        assertThat(prompt).as("F-FA-15 doit être cité dans le prompt").contains("F-FA-15");
        assertThat(prompt).as("F-FA-16 doit être cité dans le prompt").contains("F-FA-16");
        assertThat(prompt).as("F-FA-17 doit être cité dans le prompt").contains("F-FA-17");
        // Non-régression
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("DA_DUREE_MARIAGE");
    }

    // U-SF-250-08-03 : le prompt système famille-FR contient FA19_* (13 codes), FA20_*, FA21_, FA22_*
    @Test
    void systemPrompt_containsFa19Fa20Fa21Fa22CriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE", l);
        assertThat(prompt).as("FA19_REGIME_EXERCICE_ACTUEL doit être dans le prompt").contains("FA19_REGIME_EXERCICE_ACTUEL");
        assertThat(prompt).as("FA19_DANGER_CARACTERISE doit être dans le prompt").contains("FA19_DANGER_CARACTERISE");
        assertThat(prompt).as("FA19_CONSENTEMENT_AUTRE_PARENT doit être dans le prompt").contains("FA19_CONSENTEMENT_AUTRE_PARENT");
        assertThat(prompt).as("FA19_INTERFERENCE_VIE_ENFANT doit être dans le prompt").contains("FA19_INTERFERENCE_VIE_ENFANT");
        assertThat(prompt).as("FA19_AGE_ENFANTS doit être dans le prompt").contains("FA19_AGE_ENFANTS");
        assertThat(prompt).as("FA19_RAISON_CHANGEMENT doit être dans le prompt").contains("FA19_RAISON_CHANGEMENT");
        assertThat(prompt).as("FA19_INFORME_PREALABLEMENT doit être dans le prompt").contains("FA19_INFORME_PREALABLEMENT");
        assertThat(prompt).as("FA19_MODE_RESIDENCE_ACTUEL doit être dans le prompt").contains("FA19_MODE_RESIDENCE_ACTUEL");
        assertThat(prompt).as("FA19_DOMAINE_DESACCORD doit être dans le prompt").contains("FA19_DOMAINE_DESACCORD");
        assertThat(prompt).as("FA19_INTENSITE_DESACCORD doit être dans le prompt").contains("FA19_INTENSITE_DESACCORD");
        assertThat(prompt).as("FA19_TENTATIVES_MEDIATION doit être dans le prompt").contains("FA19_TENTATIVES_MEDIATION");
        assertThat(prompt).as("FA19_AGE_ENFANTS_CONCERNES doit être dans le prompt").contains("FA19_AGE_ENFANTS_CONCERNES");
        assertThat(prompt).as("FA19_URGENCE doit être dans le prompt").contains("FA19_URGENCE");
        assertThat(prompt).as("FA20_MODE_DISSOLUTION doit être dans le prompt").contains("FA20_MODE_DISSOLUTION");
        assertThat(prompt).as("FA20_REGIME_BIENS doit être dans le prompt").contains("FA20_REGIME_BIENS");
        assertThat(prompt).as("FA20_CREANCES_ALLEGUEES doit être dans le prompt").contains("FA20_CREANCES_ALLEGUEES");
        assertThat(prompt).as("FA21_DATE_JUGEMENT_SEPARATION doit être dans le prompt").contains("FA21_DATE_JUGEMENT_SEPARATION");
        assertThat(prompt).as("FA22_DATE_ORIGINE doit être dans le prompt").contains("FA22_DATE_ORIGINE");
        assertThat(prompt).as("FA22_OCCUPATION doit être dans le prompt").contains("FA22_OCCUPATION");
        // Outils cités
        assertThat(prompt).as("F-FA-19 doit être cité dans le prompt").contains("F-FA-19");
        assertThat(prompt).as("F-FA-20 doit être cité dans le prompt").contains("F-FA-20");
        assertThat(prompt).as("F-FA-21 doit être cité dans le prompt").contains("F-FA-21");
        assertThat(prompt).as("F-FA-22 doit être cité dans le prompt").contains("F-FA-22");
        // Non-régression
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("FA15_REGIME_MATRIMONIAL");
    }

    // SF-250-10 — Remédiation critereCode lot Famille BE + audit outils ⚠️

    // U-SF-250-10-01 : le prompt système famille-BE contient DESU_BE_*, F217_* annotés BELGIQUE UNIQUEMENT
    @Test
    void systemPrompt_containsFamilleBeCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "BELGIQUE", l);
        assertThat(prompt).as("DESU_BE_DATE_SEPARATION doit être dans le prompt").contains("DESU_BE_DATE_SEPARATION");
        assertThat(prompt).as("DESU_BE_CONSENTEE doit être dans le prompt").contains("DESU_BE_CONSENTEE");
        assertThat(prompt).as("DESU_BE_DATE_ASSIGNATION doit être dans le prompt").contains("DESU_BE_DATE_ASSIGNATION");
        assertThat(prompt).as("F217_DATE_MARIAGE doit être dans le prompt").contains("F217_DATE_MARIAGE");
        assertThat(prompt).as("F217_DATE_NOTIFICATION_PROJET doit être dans le prompt").contains("F217_DATE_NOTIFICATION_PROJET");
        assertThat(prompt).as("Annotation BELGIQUE UNIQUEMENT requise pour codes famille BE").contains("BELGIQUE UNIQUEMENT");
        // Non-régression codes antérieurs
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("BE_CHOIX_AVOCAT");
    }

    // U-SF-250-10-02 : le prompt système immigration-FR contient IM21_DATE_NOTIFICATION_PLACEMENT, IM22_*, IM23_*
    @Test
    void systemPrompt_containsJldDublinCrrvCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE", l);
        assertThat(prompt).as("IM21_DATE_NOTIFICATION_PLACEMENT doit être dans le prompt").contains("IM21_DATE_NOTIFICATION_PLACEMENT");
        assertThat(prompt).as("IM21_PLACEMENT_CRA doit être dans le prompt").contains("IM21_PLACEMENT_CRA");
        assertThat(prompt).as("IM21_MOTIF_PLACEMENT doit être dans le prompt").contains("IM21_MOTIF_PLACEMENT");
        assertThat(prompt).as("IM22_DATE_NOTIFICATION_DUBLIN doit être dans le prompt").contains("IM22_DATE_NOTIFICATION_DUBLIN");
        assertThat(prompt).as("IM23_DATE_NOTIFICATION_REFUS doit être dans le prompt").contains("IM23_DATE_NOTIFICATION_REFUS");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour codes JLD/Dublin/CRRV").contains("FRANCE UNIQUEMENT");
        // Non-régression codes IM21 existants
        assertThat(prompt).contains("IM21_REGULARITE_SEJOUR_FR");
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
    }

    // U-SF-250-10-03 : le prompt système famille-FR contient codes filiation, adoption, réserve, rapport
    @Test
    void systemPrompt_containsFiliationAdoptionReserveCriteraCodes() {
        AnalysisLimitsProperties.LevelLimits l = new AnalysisLimitsProperties.LevelLimits();
        l.setFaits(7); l.setPointsJuridiques(5); l.setRisques(5); l.setQuestionsOuvertes(5); l.setTimeline(5);
        String prompt = CaseAnalysisService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE", l);
        // Reconnaissance + contestation + recherche paternité
        assertThat(prompt).as("CONSENTEMENT_LIBRE doit être dans le prompt").contains("CONSENTEMENT_LIBRE");
        assertThat(prompt).as("PATERNITE_VRAISEMBLABLE doit être dans le prompt").contains("PATERNITE_VRAISEMBLABLE");
        assertThat(prompt).as("CONTESTATION_PATERNITE_MOTIFS_SERIEUX doit être dans le prompt").contains("CONTESTATION_PATERNITE_MOTIFS_SERIEUX");
        assertThat(prompt).as("CONTESTATION_PATERNITE_EXPERTISE_ADN doit être dans le prompt").contains("CONTESTATION_PATERNITE_EXPERTISE_ADN");
        assertThat(prompt).as("CONTESTATION_PATERNITE_POSSESSION_ETAT doit être dans le prompt").contains("CONTESTATION_PATERNITE_POSSESSION_ETAT");
        assertThat(prompt).as("RECHERCHE_PATERNITE_POSSESSION_ETAT doit être dans le prompt").contains("RECHERCHE_PATERNITE_POSSESSION_ETAT");
        assertThat(prompt).as("RECHERCHE_PATERNITE_EXPERTISE_ADN doit être dans le prompt").contains("RECHERCHE_PATERNITE_EXPERTISE_ADN");
        assertThat(prompt).as("RECHERCHE_PATERNITE_REFUS_ADN doit être dans le prompt").contains("RECHERCHE_PATERNITE_REFUS_ADN");
        assertThat(prompt).as("RECHERCHE_PATERNITE_MOTIFS_SERIEUX doit être dans le prompt").contains("RECHERCHE_PATERNITE_MOTIFS_SERIEUX");
        // Possession d'état
        assertThat(prompt).as("POSSESSION_ETAT_TRACTATUS doit être dans le prompt").contains("POSSESSION_ETAT_TRACTATUS");
        assertThat(prompt).as("POSSESSION_ETAT_FAMA doit être dans le prompt").contains("POSSESSION_ETAT_FAMA");
        assertThat(prompt).as("POSSESSION_ETAT_CONTINUE doit être dans le prompt").contains("POSSESSION_ETAT_CONTINUE");
        assertThat(prompt).as("POSSESSION_ETAT_PAISIBLE doit être dans le prompt").contains("POSSESSION_ETAT_PAISIBLE");
        assertThat(prompt).as("POSSESSION_ETAT_NON_EQUIVOQUE doit être dans le prompt").contains("POSSESSION_ETAT_NON_EQUIVOQUE");
        // Adoption
        assertThat(prompt).as("ADOPTION_FORME doit être dans le prompt").contains("ADOPTION_FORME");
        assertThat(prompt).as("ADOPTION_PUPILLE_ETAT doit être dans le prompt").contains("ADOPTION_PUPILLE_ETAT");
        assertThat(prompt).as("ADOPTION_ADOPTANT_MARIE doit être dans le prompt").contains("ADOPTION_ADOPTANT_MARIE");
        assertThat(prompt).as("ADOPTION_AGE_ADOPTANT doit être dans le prompt").contains("ADOPTION_AGE_ADOPTANT");
        assertThat(prompt).as("ADOPTION_AGE_ADOPTE doit être dans le prompt").contains("ADOPTION_AGE_ADOPTE");
        // Réserve héréditaire + rapport à succession
        assertThat(prompt).as("RESERVE_CONJOINT_SURVIVANT doit être dans le prompt").contains("RESERVE_CONJOINT_SURVIVANT");
        assertThat(prompt).as("RAPPORT_QUALITE_HERITIER doit être dans le prompt").contains("RAPPORT_QUALITE_HERITIER");
        assertThat(prompt).as("RAPPORT_DONATION_NOMINALE doit être dans le prompt").contains("RAPPORT_DONATION_NOMINALE");
        assertThat(prompt).as("RAPPORT_VALEUR_PARTAGE doit être dans le prompt").contains("RAPPORT_VALEUR_PARTAGE");
        assertThat(prompt).as("RAPPORT_DATE_DONATION doit être dans le prompt").contains("RAPPORT_DATE_DONATION");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour codes filiation").contains("FRANCE UNIQUEMENT");
        // Non-régression SF-250-09
        assertThat(prompt).contains("TESTAMENT_FORME");
        assertThat(prompt).contains("FA25_DATE_CERTIFICAT");
        assertThat(prompt).contains("FA06_MODE_GARDE");
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
