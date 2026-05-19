package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AiQuestionServiceTest {

    private final CaseAnalysisRepository caseAnalysisRepository = mock(CaseAnalysisRepository.class);
    private final CaseFileRepository caseFileRepository = mock(CaseFileRepository.class);
    private final AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
    private final AnalysisJobRepository analysisJobRepository = mock(AnalysisJobRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final UsageEventService usageEventService = mock(UsageEventService.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

    private final AiQuestionService service = new AiQuestionService(
            caseAnalysisRepository, caseFileRepository, aiQuestionRepository,
            analysisJobRepository, anthropicService, usageEventService, eventPublisher);

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
        ReflectionTestUtils.setField(service, "self", service);
        when(caseAnalysisRepository.findById(any())).thenAnswer(inv -> Optional.of(new CaseAnalysis()));
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // U-01 : génération nominale → questions persistées, job DONE
    @Test
    void consumeQuestionGeneration_nominal_persistsQuestionsAndJobDone() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setAnalysisResult("{\"faits\":[\"fait1\"]}");
        analysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"questions\":[\"Q1 ?\",\"Q2 ?\",\"Q3 ?\"]}", "claude-sonnet-4-6", 100, 50));
        when(aiQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeQuestionGeneration(new AiQuestionGenerationMessage(caseFileId));

        ArgumentCaptor<AiQuestion> questionCaptor = ArgumentCaptor.forClass(AiQuestion.class);
        verify(aiQuestionRepository, times(3)).save(questionCaptor.capture());
        List<AiQuestion> saved = questionCaptor.getAllValues();
        assertThat(saved.get(0).getQuestionText()).isEqualTo("Q1 ?");
        assertThat(saved.get(0).getOrderIndex()).isEqualTo(0);
        assertThat(saved.get(2).getOrderIndex()).isEqualTo(2);

        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, times(2)).save(jobCaptor.capture());
        AnalysisJob finalJob = jobCaptor.getValue();
        assertThat(finalJob.getStatus()).isEqualTo(AnalysisStatus.DONE);
        assertThat(finalJob.getProcessedItems()).isEqualTo(1);

        // Usage enregistré
        verify(usageEventService).record(caseFileId, userId, JobType.QUESTION_GENERATION, 100, 50);
    }

    // F-185 SF-185-02 : DONE → événement SSE QUESTION_GENERATION publié après commit
    @Test
    void finalizeQuestionGeneration_done_publishesQuestionGenerationDoneEvent() {
        UUID caseFileId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setAnalysisResult("{}");
        analysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(caseFileRepository.findCreatedByUserIdById(caseFileId)).thenReturn(Optional.of(userId));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenReturn(
                new AnthropicResult("{\"questions\":[\"Q1 ?\"]}", "claude-sonnet-4-6", 100, 50));
        when(aiQuestionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.consumeQuestionGeneration(new AiQuestionGenerationMessage(caseFileId));
        // Simulate transaction commit to trigger afterCommit callbacks
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(eventPublisher).publishEvent(new AnalysisStatusEvent(
                caseFileId, AnalysisStatus.DONE, JobType.QUESTION_GENERATION));
    }

    // F-185 SF-185-02 : FAILED → événement SSE QUESTION_GENERATION_FAILED publié
    @Test
    void finalizeQuestionGeneration_failed_publishesQuestionGenerationFailedEvent() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setAnalysisResult("{}");
        analysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

        service.consumeQuestionGeneration(new AiQuestionGenerationMessage(caseFileId));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(eventPublisher).publishEvent(new AnalysisStatusEvent(
                caseFileId, AnalysisStatus.FAILED, JobType.QUESTION_GENERATION));
    }

    // U-02 : erreur LLM → job FAILED, aucune question persistée
    @Test
    void consumeQuestionGeneration_anthropicError_jobFailed() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setAnalysisResult("{}");
        analysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

        service.consumeQuestionGeneration(new AiQuestionGenerationMessage(caseFileId));

        verifyNoInteractions(aiQuestionRepository);
        ArgumentCaptor<AnalysisJob> jobCaptor = ArgumentCaptor.forClass(AnalysisJob.class);
        verify(analysisJobRepository, times(2)).save(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(jobCaptor.getValue().getErrorMessage()).isNotNull();
    }

    // U-03 : pas de CaseAnalysis DONE → skip
    @Test
    void consumeQuestionGeneration_noCaseAnalysis_skip() {
        UUID caseFileId = UUID.randomUUID();
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.empty());

        service.consumeQuestionGeneration(new AiQuestionGenerationMessage(caseFileId));

        verifyNoInteractions(caseFileRepository, aiQuestionRepository, analysisJobRepository, anthropicService);
    }

    // U-04 : parseQuestions — parsing nominal (format legacy string)
    @Test
    void parseQuestions_legacyStringFormat_returnsQuestionsWithoutCode() {
        var result = AiQuestionService.parseQuestions(
                "{\"questions\":[\"Q1 ?\",\"Q2 ?\"]}");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).text()).isEqualTo("Q1 ?");
        assertThat(result.get(0).critereCode()).isNull();
        assertThat(result.get(1).text()).isEqualTo("Q2 ?");
    }

    // U-05 : parseQuestions — JSON malformé → liste vide
    @Test
    void parseQuestions_malformed_returnsEmptyList() {
        assertThat(AiQuestionService.parseQuestions("not json")).isEmpty();
        assertThat(AiQuestionService.parseQuestions("{\"other\":[]}")).isEmpty();
    }

    // U-06 : parseQuestions — format objet avec critere_code
    @Test
    void parseQuestions_objectFormatWithCritereCode_returnsCodeUpperCase() {
        var result = AiQuestionService.parseQuestions(
                "{\"questions\":[{\"texte\":\"LRAR envoyée ?\",\"critere_code\":\"fr_convocation\"},{\"texte\":\"Motivation précise ?\",\"critere_code\":null}]}");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).text()).isEqualTo("LRAR envoyée ?");
        assertThat(result.get(0).critereCode()).isEqualTo("FR_CONVOCATION");
        assertThat(result.get(1).text()).isEqualTo("Motivation précise ?");
        assertThat(result.get(1).critereCode()).isNull();
    }

    // U-07 : parseQuestions — mix string + objet
    @Test
    void parseQuestions_mixedFormats_parsesBoth() {
        var result = AiQuestionService.parseQuestions(
                "{\"questions\":[\"Legacy\",{\"texte\":\"Objet\",\"critere_code\":\"FR_ENTRETIEN\"}]}");
        assertThat(result).hasSize(2);
        assertThat(result.get(0).critereCode()).isNull();
        assertThat(result.get(1).critereCode()).isEqualTo("FR_ENTRETIEN");
    }

    // U-08 : parseQuestions — texte blank ignoré
    @Test
    void parseQuestions_blankOrMissingTexte_isIgnored() {
        var result = AiQuestionService.parseQuestions(
                "{\"questions\":[{\"texte\":\"\",\"critere_code\":\"FR_CONVOCATION\"},{\"critere_code\":\"FR_ENTRETIEN\"}]}");
        assertThat(result).isEmpty();
    }

    // SF-250-02 — Remédiation critereCode lot Travail FR validité/procédure

    // U-SF-250-02-01 : le prompt questions travail-FR contient les 3 codes DT36_*
    @Test
    void systemPrompt_containsDt36CodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).as("DT36_DATE_ENTRETIEN doit être dans le prompt questions").contains("DT36_DATE_ENTRETIEN");
        assertThat(prompt).as("DT36_MOTIVATION doit être dans le prompt questions").contains("DT36_MOTIVATION");
        assertThat(prompt).as("DT36_ENTRETIEN_TENU doit être dans le prompt questions").contains("DT36_ENTRETIEN_TENU");
        // Convention impérative réponse "oui" = signal positif doit être présente
        assertThat(prompt).as("Convention réponse oui doit être documentée").contains("oui");
    }

    // U-SF-250-02-02 : le prompt questions travail-FR contient HLN_, DT13_, PSE_, PROTECTION_RP_
    @Test
    void systemPrompt_containsHlnDt13PseProtectionRpCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).as("HLN_MOTIF_NULLITE doit être dans le prompt questions").contains("HLN_MOTIF_NULLITE");
        assertThat(prompt).as("DT13_MOTIF_ECONOMIQUE doit être dans le prompt questions").contains("DT13_MOTIF_ECONOMIQUE");
        assertThat(prompt).as("DT13_DATE_NOTIFICATION doit être dans le prompt questions").contains("DT13_DATE_NOTIFICATION");
        assertThat(prompt).as("PSE_DATE_PROJET doit être dans le prompt questions").contains("PSE_DATE_PROJET");
        assertThat(prompt).as("PROTECTION_RP_MOTIF doit être dans le prompt questions").contains("PROTECTION_RP_MOTIF");
        // Non-régression : codes existants toujours présents
        assertThat(prompt).contains("FR_CONVOCATION");
        assertThat(prompt).contains("DT09_TYPE_RUPTURE");
    }

    // SF-250-03 — Remédiation critereCode lot Travail FR rupture/requalifications/inaptitude/AT-MP

    // U-SF-250-03-01 : le prompt questions travail-FR contient les 6 codes RC_*
    @Test
    void systemPrompt_containsRcCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).as("RC_CONSENTEMENT doit être dans le prompt questions").contains("RC_CONSENTEMENT");
        assertThat(prompt).as("RC_DELAI_RETRACTATION doit être dans le prompt questions").contains("RC_DELAI_RETRACTATION");
        assertThat(prompt).as("RC_HOMOLOGATION doit être dans le prompt questions").contains("RC_HOMOLOGATION");
        assertThat(prompt).as("RC_ASSISTANCE doit être dans le prompt questions").contains("RC_ASSISTANCE");
        assertThat(prompt).as("RC_INDEMNITE doit être dans le prompt questions").contains("RC_INDEMNITE");
        assertThat(prompt).as("RC_ENTRETIENS doit être dans le prompt questions").contains("RC_ENTRETIENS");
        // Non-régression
        assertThat(prompt).contains("FR_CONVOCATION");
        assertThat(prompt).contains("DT36_DATE_ENTRETIEN");
    }

    // U-SF-250-03-02 : le prompt questions travail-FR contient DT22_, DT23_, DT24_, DT31_, RCI_
    @Test
    void systemPrompt_containsDt22Dt23Dt24Dt31RciCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).as("DT22_SALAIRE doit être dans le prompt questions").contains("DT22_SALAIRE");
        assertThat(prompt).as("DT23_SALAIRE doit être dans le prompt questions").contains("DT23_SALAIRE");
        assertThat(prompt).as("DT24_SALAIRE doit être dans le prompt questions").contains("DT24_SALAIRE");
        assertThat(prompt).as("DT31_SALAIRE_MENSUEL doit être dans le prompt questions").contains("DT31_SALAIRE_MENSUEL");
        assertThat(prompt).as("DT31_ANCIENNETE doit être dans le prompt questions").contains("DT31_ANCIENNETE");
        assertThat(prompt).as("RCI_SALAIRE doit être dans le prompt questions").contains("RCI_SALAIRE");
        assertThat(prompt).as("RCI_ANCIENNETE doit être dans le prompt questions").contains("RCI_ANCIENNETE");
        // Non-régression
        assertThat(prompt).contains("DT09_TYPE_RUPTURE");
        assertThat(prompt).contains("HLN_MOTIF_NULLITE");
    }

    // U-SF-250-03-03 : le prompt questions travail-FR contient INAPT_ et AT_MP_
    @Test
    void systemPrompt_containsInapt_andAtMpCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).as("INAPT_ORIGINE doit être dans le prompt questions").contains("INAPT_ORIGINE");
        assertThat(prompt).as("INAPT_RECLASSEMENT doit être dans le prompt questions").contains("INAPT_RECLASSEMENT");
        assertThat(prompt).as("AT_MP_DATE_ACCIDENT doit être dans le prompt questions").contains("AT_MP_DATE_ACCIDENT");
        // Convention réponse "oui" = signal positif documentée
        assertThat(prompt).contains("oui");
        // Non-régression
        assertThat(prompt).contains("FR_CONVOCATION");
        assertThat(prompt).contains("RC_CONSENTEMENT");
    }

    // SF-250-07 — Remédiation critereCode lot Immigration BE

    // U-SF-250-07-01 : le prompt questions immigration-BE contient IM08_MOTIF_OQT_BE annoté BELGIQUE UNIQUEMENT
    @Test
    void systemPrompt_containsIm08OqtBeCodeForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "BELGIQUE");
        assertThat(prompt).as("IM08_MOTIF_OQT_BE doit être dans le prompt questions").contains("IM08_MOTIF_OQT_BE");
        assertThat(prompt).as("Annotation BELGIQUE UNIQUEMENT requise pour IM08_MOTIF_OQT_BE").contains("BELGIQUE UNIQUEMENT");
        // Non-régression codes FR
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
        assertThat(prompt).contains("IM11_TITRE_ACTUEL");
    }

    // U-SF-250-07-02 : le prompt questions immigration contient BE_9TER_* et IM17_VOIE_REGIME_ALGERIEN
    @Test
    void systemPrompt_containsBe9terAndIm17CodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "BELGIQUE");
        assertThat(prompt).as("BE_9TER_MALADIE_GRAVE doit être dans le prompt questions").contains("BE_9TER_MALADIE_GRAVE");
        assertThat(prompt).as("BE_9TER_SOINS_BE doit être dans le prompt questions").contains("BE_9TER_SOINS_BE");
        assertThat(prompt).as("BE_9TER_SOINS_INACCESSIBLES doit être dans le prompt questions").contains("BE_9TER_SOINS_INACCESSIBLES");
        assertThat(prompt).as("BE_9TER_MENACE_ORDRE_PUBLIC doit être dans le prompt questions").contains("BE_9TER_MENACE_ORDRE_PUBLIC");
        assertThat(prompt).as("IM17_VOIE_REGIME_ALGERIEN doit être dans le prompt questions").contains("IM17_VOIE_REGIME_ALGERIEN");
        // Non-régression codes SF-250-05, SF-250-06
        assertThat(prompt).contains("IM24_DATE_ORDONNANCE_PROTECTION");
        assertThat(prompt).contains("IM20_DISPOSITIF_ELOIGNEMENT");
    }

    // SF-250-06 — Remédiation critereCode lot Immigration FR — titres / asile / éloignement

    // U-SF-250-06-01 : le prompt questions immigration-FR contient les codes IM11_, IM12_, IM13_
    @Test
    void systemPrompt_containsIm11Im12Im13CodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE");
        assertThat(prompt).as("IM11_TITRE_ACTUEL doit être dans le prompt questions").contains("IM11_TITRE_ACTUEL");
        assertThat(prompt).as("IM12_DISPOSITIF_ASILE doit être dans le prompt questions").contains("IM12_DISPOSITIF_ASILE");
        assertThat(prompt).as("IM13_VOIE_NATURALISATION doit être dans le prompt questions").contains("IM13_VOIE_NATURALISATION");
        // Non-régression codes SF-250-05
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
        assertThat(prompt).contains("IM09H_MOTIF_HUMANITAIRE");
    }

    // U-SF-250-06-02 : le prompt questions immigration-FR contient les codes IM19_* (mineurs)
    @Test
    void systemPrompt_containsIm19MineursCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE");
        assertThat(prompt).as("IM19_DATE_NAISSANCE doit être dans le prompt questions").contains("IM19_DATE_NAISSANCE");
        assertThat(prompt).as("IM19_DATE_ENTREE doit être dans le prompt questions").contains("IM19_DATE_ENTREE");
        // Non-régression
        assertThat(prompt).contains("IM08_RECOURS_FORME");
    }

    // U-SF-250-06-03 : le prompt questions immigration-FR contient IM20_* et IM24_*
    @Test
    void systemPrompt_containsIm20EloignementIm24ViolencesCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE");
        assertThat(prompt).as("IM20_DISPOSITIF_ELOIGNEMENT doit être dans le prompt questions").contains("IM20_DISPOSITIF_ELOIGNEMENT");
        assertThat(prompt).as("IM20_MOTIF_MENACE doit être dans le prompt questions").contains("IM20_MOTIF_MENACE");
        assertThat(prompt).as("IM24_DATE_ORDONNANCE_PROTECTION doit être dans le prompt questions").contains("IM24_DATE_ORDONNANCE_PROTECTION");
        // Non-régression codes SF-250-04 + SF-250-05
        assertThat(prompt).contains("CREDIT_TEMPS_ANCIENNETE");
        assertThat(prompt).contains("TRAVAIL_PROCEDURE_TYPE");
        assertThat(prompt).contains("IM09_DATE_ENTREE_FRANCE");
    }

    // SF-250-05 — Remédiation critereCode lot Immigration FR — OQTF / AES

    // U-SF-250-05-01 : le prompt questions immigration-FR contient les codes IM08_* (OQTF + référés)
    @Test
    void systemPrompt_containsIm08CodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE");
        assertThat(prompt).as("IM08_MOTIF_OQTF doit être dans le prompt questions").contains("IM08_MOTIF_OQTF");
        assertThat(prompt).as("IM08_RECOURS_FORME doit être dans le prompt questions").contains("IM08_RECOURS_FORME");
        assertThat(prompt).as("IM08RA_DECISION_CONTESTEE doit être dans le prompt questions").contains("IM08RA_DECISION_CONTESTEE");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour IM08").contains("FRANCE UNIQUEMENT");
        // Non-régression
        assertThat(prompt).contains("IM05_MOTIF");
        assertThat(prompt).contains("IM06_RECOURS_TYPE");
    }

    // U-SF-250-05-02 : le prompt questions immigration-FR contient les codes IM09_* métiers tension
    @Test
    void systemPrompt_containsIm09MetiersCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE");
        assertThat(prompt).as("IM09_DATE_ENTREE_FRANCE doit être dans le prompt questions").contains("IM09_DATE_ENTREE_FRANCE");
        assertThat(prompt).as("IM09_MOIS_ACTIVITE doit être dans le prompt questions").contains("IM09_MOIS_ACTIVITE");
        // Non-régression
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
    }

    // U-SF-250-05-03 : le prompt questions immigration-FR contient les codes IM09_ETU_* et IM09H_*
    @Test
    void systemPrompt_containsIm09EtudiantHumanitaireCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE");
        assertThat(prompt).as("IM09_ETU_DATE_ENTREE_FRANCE doit être dans le prompt questions").contains("IM09_ETU_DATE_ENTREE_FRANCE");
        assertThat(prompt).as("IM09_ETU_DUREE_PRESENCE doit être dans le prompt questions").contains("IM09_ETU_DUREE_PRESENCE");
        assertThat(prompt).as("IM09_ETU_DATE_DEPOT_DEMANDE doit être dans le prompt questions").contains("IM09_ETU_DATE_DEPOT_DEMANDE");
        assertThat(prompt).as("IM09H_DATE_ENTREE_FRANCE doit être dans le prompt questions").contains("IM09H_DATE_ENTREE_FRANCE");
        assertThat(prompt).as("IM09H_MOTIF_HUMANITAIRE doit être dans le prompt questions").contains("IM09H_MOTIF_HUMANITAIRE");
        // Non-régression codes SF-250-04
        assertThat(prompt).contains("CREDIT_TEMPS_ANCIENNETE");
        assertThat(prompt).contains("TRAVAIL_PROCEDURE_TYPE");
    }

    // SF-250-04 — Remédiation critereCode lot Travail BE + F-136

    // U-SF-250-04-01 : le prompt questions travail contient les codes CREDIT_TEMPS_* annotés BELGIQUE UNIQUEMENT
    @Test
    void systemPrompt_containsCreditTempsCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DU_TRAVAIL", "BELGIQUE");
        assertThat(prompt).as("CREDIT_TEMPS_ANCIENNETE doit être dans le prompt questions").contains("CREDIT_TEMPS_ANCIENNETE");
        assertThat(prompt).as("CREDIT_TEMPS_AGE doit être dans le prompt questions").contains("CREDIT_TEMPS_AGE");
        assertThat(prompt).as("Annotation BELGIQUE UNIQUEMENT requise pour CREDIT_TEMPS").contains("BELGIQUE UNIQUEMENT");
        // Non-régression
        assertThat(prompt).contains("AT_MP_DATE_ACCIDENT");
        assertThat(prompt).contains("DT36_DATE_ENTRETIEN");
    }

    // U-SF-250-04-02 : le prompt questions travail contient TRAVAIL_PROCEDURE_TYPE
    @Test
    void systemPrompt_containsTravailProcedureTypeForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DU_TRAVAIL", "FRANCE");
        assertThat(prompt).as("TRAVAIL_PROCEDURE_TYPE doit être dans le prompt questions").contains("TRAVAIL_PROCEDURE_TYPE");
        // Convention réponse "oui" = signal positif documentée
        assertThat(prompt).contains("oui");
        // Non-régression
        assertThat(prompt).contains("FR_CONVOCATION");
        assertThat(prompt).contains("INAPT_ORIGINE");
    }
}
