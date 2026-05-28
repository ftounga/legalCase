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
        // F-257 — pré-résolution AiCallContext user-level. Les tests qui exercent
        // consumeQuestionGeneration utilisent `new CaseFile()` sans workspace ; on stube
        // ici les fallbacks DB pour que workspaceId + userId soient résolus par défaut
        // (sinon SKIP early → FAILED, anthropicService jamais appelé).
        when(caseFileRepository.findWorkspaceIdById(any())).thenReturn(Optional.of(UUID.randomUUID()));
        when(caseFileRepository.findCreatedByUserIdById(any())).thenReturn(Optional.of(UUID.randomUUID()));
    }

    @AfterEach
    void clearTransactionSync() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    // U-01 : génération nominale → questions persistées, job DONE
    @Test
    void consumeQuestionGeneration_nominal_persistsQuestionsAndJobDone() {
        UUID caseFileId = UUID.randomUUID();
        CaseFile caseFile = new CaseFile();

        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setAnalysisResult("{\"faits\":[\"fait1\"]}");
        analysis.setAnalysisStatus(AnalysisStatus.DONE);

        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                caseFileId, AnalysisStatus.DONE)).thenReturn(Optional.of(analysis));
        when(caseFileRepository.findById(caseFileId)).thenReturn(Optional.of(caseFile));
        when(analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.QUESTION_GENERATION))
                .thenReturn(Optional.empty());
        when(analysisJobRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
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

        // F-257 — record automatique dans AnthropicService.analyzeWithSystemCache, plus de record direct ici.
        verifyNoInteractions(usageEventService);
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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenReturn(
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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

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
        when(anthropicService.analyzeWithSystemCache(any(AiCallContext.class), any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

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

    // SF-250-09 — Remédiation critereCode lot Famille FR — successions / libéralités

    // U-SF-250-09-01 : le prompt questions famille-FR contient TESTAMENT_* et DONATION_*
    @Test
    void systemPrompt_containsTestamentDonationCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE");
        assertThat(prompt).as("TESTAMENT_FORME doit être dans le prompt questions").contains("TESTAMENT_FORME");
        assertThat(prompt).as("TESTAMENT_SAINE_ESPRIT doit être dans le prompt questions").contains("TESTAMENT_SAINE_ESPRIT");
        assertThat(prompt).as("TESTAMENT_QUOTITE doit être dans le prompt questions").contains("TESTAMENT_QUOTITE");
        assertThat(prompt).as("DONATION_FORME doit être dans le prompt questions").contains("DONATION_FORME");
        assertThat(prompt).as("DONATION_SAINE_ESPRIT doit être dans le prompt questions").contains("DONATION_SAINE_ESPRIT");
        assertThat(prompt).as("DONATION_QUOTITE doit être dans le prompt questions").contains("DONATION_QUOTITE");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour codes successions").contains("FRANCE UNIQUEMENT");
        // Non-régression codes SF-250-08
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("FA22_OCCUPATION");
    }

    // U-SF-250-09-02 : le prompt questions famille-FR contient PARTAGE_*, INDIVISION_*, DEVOLUTION_LEGALE_*
    @Test
    void systemPrompt_containsPartageIndivisionDevolutionCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE");
        assertThat(prompt).as("PARTAGE_MODE doit être dans le prompt questions").contains("PARTAGE_MODE");
        assertThat(prompt).as("PARTAGE_CONSENTEMENTS doit être dans le prompt questions").contains("PARTAGE_CONSENTEMENTS");
        assertThat(prompt).as("PARTAGE_PRESENCE_IMMEUBLES doit être dans le prompt questions").contains("PARTAGE_PRESENCE_IMMEUBLES");
        assertThat(prompt).as("INDIVISION_DATE_OUVERTURE doit être dans le prompt questions").contains("INDIVISION_DATE_OUVERTURE");
        assertThat(prompt).as("INDIVISION_TYPE doit être dans le prompt questions").contains("INDIVISION_TYPE");
        assertThat(prompt).as("DEVOLUTION_LEGALE_CONJOINT doit être dans le prompt questions").contains("DEVOLUTION_LEGALE_CONJOINT");
        assertThat(prompt).as("DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS doit être dans le prompt questions").contains("DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS");
        // Non-régression
        assertThat(prompt).contains("TESTAMENT_FORME");
        assertThat(prompt).contains("FA22_DATE_ORIGINE");
    }

    // U-SF-250-09-03 : le prompt questions famille-FR contient FA25_*, FA26_*, PMA_GPA_DISPOSITIF, FA05_*
    @Test
    void systemPrompt_containsFa25Fa26PmaGpaFa05CodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE");
        assertThat(prompt).as("FA25_DATE_CERTIFICAT doit être dans le prompt questions").contains("FA25_DATE_CERTIFICAT");
        assertThat(prompt).as("FA25_ALT_MENTALES doit être dans le prompt questions").contains("FA25_ALT_MENTALES");
        assertThat(prompt).as("FA25_CONSENTEMENT doit être dans le prompt questions").contains("FA25_CONSENTEMENT");
        assertThat(prompt).as("FA25_DEMANDEUR_FAMILIAL doit être dans le prompt questions").contains("FA25_DEMANDEUR_FAMILIAL");
        assertThat(prompt).as("FA26_TYPE_CHANGEMENT doit être dans le prompt questions").contains("FA26_TYPE_CHANGEMENT");
        assertThat(prompt).as("FA26_MOTIF_INVOQUE doit être dans le prompt questions").contains("FA26_MOTIF_INVOQUE");
        assertThat(prompt).as("FA26_DATE_NAISSANCE doit être dans le prompt questions").contains("FA26_DATE_NAISSANCE");
        assertThat(prompt).as("FA26_MAJEUR_DEMANDEUR doit être dans le prompt questions").contains("FA26_MAJEUR_DEMANDEUR");
        assertThat(prompt).as("FA26_CONSENTEMENT_PARENTAL doit être dans le prompt questions").contains("FA26_CONSENTEMENT_PARENTAL");
        assertThat(prompt).as("PMA_GPA_DISPOSITIF doit être dans le prompt questions").contains("PMA_GPA_DISPOSITIF");
        assertThat(prompt).as("FA05_VALEUR_VENALE doit être dans le prompt questions").contains("FA05_VALEUR_VENALE");
        assertThat(prompt).as("FA05_CAPITAL_RESTANT doit être dans le prompt questions").contains("FA05_CAPITAL_RESTANT");
        // Non-régression
        assertThat(prompt).contains("TESTAMENT_FORME");
        assertThat(prompt).contains("PARTAGE_MODE");
        assertThat(prompt).contains("FA22_OCCUPATION");
    }

    // SF-250-08 — Remédiation critereCode lot Famille FR — divorce / union

    // U-SF-250-08-01 : le prompt questions famille-FR contient DA_DUREE_MARIAGE, FA09_*, FA12_*, FA13_, FA14_*
    @Test
    void systemPrompt_containsDaFa09Fa12Fa13Fa14CodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE");
        assertThat(prompt).as("DA_DUREE_MARIAGE doit être dans le prompt questions").contains("DA_DUREE_MARIAGE");
        assertThat(prompt).as("FA09_DUREE_MARIAGE doit être dans le prompt questions").contains("FA09_DUREE_MARIAGE");
        assertThat(prompt).as("FA09_DATE_DEPOT_ASSIGNATION doit être dans le prompt questions").contains("FA09_DATE_DEPOT_ASSIGNATION");
        assertThat(prompt).as("FA09_FAUTES_INVOQUEES doit être dans le prompt questions").contains("FA09_FAUTES_INVOQUEES");
        assertThat(prompt).as("FA12_DATE_AUDIENCE doit être dans le prompt questions").contains("FA12_DATE_AUDIENCE");
        assertThat(prompt).as("FA12_VIOLENCES doit être dans le prompt questions").contains("FA12_VIOLENCES");
        assertThat(prompt).as("FA13_NB_ENFANTS doit être dans le prompt questions").contains("FA13_NB_ENFANTS");
        assertThat(prompt).as("FA14_DATE_REQUETE doit être dans le prompt questions").contains("FA14_DATE_REQUETE");
        assertThat(prompt).as("FA14_VIOLENCES_ALLEGUEES doit être dans le prompt questions").contains("FA14_VIOLENCES_ALLEGUEES");
        assertThat(prompt).as("FA14_LOGEMENT_COMMUN doit être dans le prompt questions").contains("FA14_LOGEMENT_COMMUN");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise").contains("FRANCE UNIQUEMENT");
        // Non-régression
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("FR_CHOIX_AVOCATS");
    }

    // U-SF-250-08-02 : le prompt questions famille-FR contient FA15_, COMMUNAUTE_UNIVERSELLE_*, PARTAGE_JUDICIAIRE_*
    @Test
    void systemPrompt_containsFa15CommunauteUniversellePartageJudiciairCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE");
        assertThat(prompt).as("FA15_REGIME_MATRIMONIAL doit être dans le prompt questions").contains("FA15_REGIME_MATRIMONIAL");
        assertThat(prompt).as("COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE doit être dans le prompt questions").contains("COMMUNAUTE_UNIVERSELLE_CONTRAT_NOTARIE");
        assertThat(prompt).as("COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS doit être dans le prompt questions").contains("COMMUNAUTE_UNIVERSELLE_ENFANTS_NON_COMMUNS");
        assertThat(prompt).as("PARTAGE_JUDICIAIRE_PV doit être dans le prompt questions").contains("PARTAGE_JUDICIAIRE_PV");
        assertThat(prompt).as("PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE doit être dans le prompt questions").contains("PARTAGE_JUDICIAIRE_TENTATIVE_AMIABLE");
        // Non-régression
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("DA_DUREE_MARIAGE");
    }

    // U-SF-250-08-03 : le prompt questions famille-FR contient FA19_* (13 codes), FA20_*, FA21_, FA22_*
    @Test
    void systemPrompt_containsFa19Fa20Fa21Fa22CodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE");
        assertThat(prompt).as("FA19_REGIME_EXERCICE_ACTUEL doit être dans le prompt questions").contains("FA19_REGIME_EXERCICE_ACTUEL");
        assertThat(prompt).as("FA19_DANGER_CARACTERISE doit être dans le prompt questions").contains("FA19_DANGER_CARACTERISE");
        assertThat(prompt).as("FA19_CONSENTEMENT_AUTRE_PARENT doit être dans le prompt questions").contains("FA19_CONSENTEMENT_AUTRE_PARENT");
        assertThat(prompt).as("FA19_INTERFERENCE_VIE_ENFANT doit être dans le prompt questions").contains("FA19_INTERFERENCE_VIE_ENFANT");
        assertThat(prompt).as("FA19_AGE_ENFANTS doit être dans le prompt questions").contains("FA19_AGE_ENFANTS");
        assertThat(prompt).as("FA19_RAISON_CHANGEMENT doit être dans le prompt questions").contains("FA19_RAISON_CHANGEMENT");
        assertThat(prompt).as("FA19_INFORME_PREALABLEMENT doit être dans le prompt questions").contains("FA19_INFORME_PREALABLEMENT");
        assertThat(prompt).as("FA19_MODE_RESIDENCE_ACTUEL doit être dans le prompt questions").contains("FA19_MODE_RESIDENCE_ACTUEL");
        assertThat(prompt).as("FA19_DOMAINE_DESACCORD doit être dans le prompt questions").contains("FA19_DOMAINE_DESACCORD");
        assertThat(prompt).as("FA19_INTENSITE_DESACCORD doit être dans le prompt questions").contains("FA19_INTENSITE_DESACCORD");
        assertThat(prompt).as("FA19_TENTATIVES_MEDIATION doit être dans le prompt questions").contains("FA19_TENTATIVES_MEDIATION");
        assertThat(prompt).as("FA19_AGE_ENFANTS_CONCERNES doit être dans le prompt questions").contains("FA19_AGE_ENFANTS_CONCERNES");
        assertThat(prompt).as("FA19_URGENCE doit être dans le prompt questions").contains("FA19_URGENCE");
        assertThat(prompt).as("FA20_MODE_DISSOLUTION doit être dans le prompt questions").contains("FA20_MODE_DISSOLUTION");
        assertThat(prompt).as("FA20_REGIME_BIENS doit être dans le prompt questions").contains("FA20_REGIME_BIENS");
        assertThat(prompt).as("FA20_CREANCES_ALLEGUEES doit être dans le prompt questions").contains("FA20_CREANCES_ALLEGUEES");
        assertThat(prompt).as("FA21_DATE_JUGEMENT_SEPARATION doit être dans le prompt questions").contains("FA21_DATE_JUGEMENT_SEPARATION");
        assertThat(prompt).as("FA22_DATE_ORIGINE doit être dans le prompt questions").contains("FA22_DATE_ORIGINE");
        assertThat(prompt).as("FA22_OCCUPATION doit être dans le prompt questions").contains("FA22_OCCUPATION");
        // Non-régression
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("FA15_REGIME_MATRIMONIAL");
    }

    // SF-250-10 — Remédiation critereCode lot Famille BE + audit outils ⚠️

    // U-SF-250-10-01 : le prompt questions famille-BE contient DESU_BE_*, F217_* annotés BELGIQUE UNIQUEMENT
    @Test
    void systemPrompt_containsFamilleBeCriteraCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "BELGIQUE");
        assertThat(prompt).as("DESU_BE_DATE_SEPARATION doit être dans le prompt questions").contains("DESU_BE_DATE_SEPARATION");
        assertThat(prompt).as("DESU_BE_CONSENTEE doit être dans le prompt questions").contains("DESU_BE_CONSENTEE");
        assertThat(prompt).as("DESU_BE_DATE_ASSIGNATION doit être dans le prompt questions").contains("DESU_BE_DATE_ASSIGNATION");
        assertThat(prompt).as("F217_DATE_MARIAGE doit être dans le prompt questions").contains("F217_DATE_MARIAGE");
        assertThat(prompt).as("F217_DATE_NOTIFICATION_PROJET doit être dans le prompt questions").contains("F217_DATE_NOTIFICATION_PROJET");
        assertThat(prompt).as("Annotation BELGIQUE UNIQUEMENT requise pour codes famille BE").contains("BELGIQUE UNIQUEMENT");
        // Non-régression
        assertThat(prompt).contains("FA06_MODE_GARDE");
        assertThat(prompt).contains("BE_CHOIX_AVOCAT");
    }

    // U-SF-250-10-02 : le prompt questions immigration-FR contient IM21_DATE_NOTIFICATION_PLACEMENT, IM22_*, IM23_*
    @Test
    void systemPrompt_containsJldDublinCrrvCriteraCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_L_IMMIGRATION", "FRANCE");
        assertThat(prompt).as("IM21_DATE_NOTIFICATION_PLACEMENT doit être dans le prompt questions").contains("IM21_DATE_NOTIFICATION_PLACEMENT");
        assertThat(prompt).as("IM21_PLACEMENT_CRA doit être dans le prompt questions").contains("IM21_PLACEMENT_CRA");
        assertThat(prompt).as("IM21_MOTIF_PLACEMENT doit être dans le prompt questions").contains("IM21_MOTIF_PLACEMENT");
        assertThat(prompt).as("IM22_DATE_NOTIFICATION_DUBLIN doit être dans le prompt questions").contains("IM22_DATE_NOTIFICATION_DUBLIN");
        assertThat(prompt).as("IM23_DATE_NOTIFICATION_REFUS doit être dans le prompt questions").contains("IM23_DATE_NOTIFICATION_REFUS");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour codes JLD/Dublin/CRRV").contains("FRANCE UNIQUEMENT");
        // Non-régression codes immigration FR existants dans AiQuestionService
        assertThat(prompt).contains("IM08_MOTIF_OQTF");
        assertThat(prompt).contains("IM11_TITRE_ACTUEL");
    }

    // U-SF-250-10-03 : le prompt questions famille-FR contient codes filiation, adoption, réserve, rapport
    @Test
    void systemPrompt_containsFiliationAdoptionReserveCriteraCodesForQuestions() {
        String prompt = AiQuestionService.buildSystemPrompt("DROIT_DE_LA_FAMILLE", "FRANCE");
        // Reconnaissance + contestation + recherche paternité
        assertThat(prompt).as("CONSENTEMENT_LIBRE doit être dans le prompt questions").contains("CONSENTEMENT_LIBRE");
        assertThat(prompt).as("PATERNITE_VRAISEMBLABLE doit être dans le prompt questions").contains("PATERNITE_VRAISEMBLABLE");
        assertThat(prompt).as("CONTESTATION_PATERNITE_MOTIFS_SERIEUX doit être dans le prompt questions").contains("CONTESTATION_PATERNITE_MOTIFS_SERIEUX");
        assertThat(prompt).as("CONTESTATION_PATERNITE_EXPERTISE_ADN doit être dans le prompt questions").contains("CONTESTATION_PATERNITE_EXPERTISE_ADN");
        assertThat(prompt).as("CONTESTATION_PATERNITE_POSSESSION_ETAT doit être dans le prompt questions").contains("CONTESTATION_PATERNITE_POSSESSION_ETAT");
        assertThat(prompt).as("RECHERCHE_PATERNITE_POSSESSION_ETAT doit être dans le prompt questions").contains("RECHERCHE_PATERNITE_POSSESSION_ETAT");
        assertThat(prompt).as("RECHERCHE_PATERNITE_EXPERTISE_ADN doit être dans le prompt questions").contains("RECHERCHE_PATERNITE_EXPERTISE_ADN");
        assertThat(prompt).as("RECHERCHE_PATERNITE_REFUS_ADN doit être dans le prompt questions").contains("RECHERCHE_PATERNITE_REFUS_ADN");
        assertThat(prompt).as("RECHERCHE_PATERNITE_MOTIFS_SERIEUX doit être dans le prompt questions").contains("RECHERCHE_PATERNITE_MOTIFS_SERIEUX");
        // Possession d'état
        assertThat(prompt).as("POSSESSION_ETAT_TRACTATUS doit être dans le prompt questions").contains("POSSESSION_ETAT_TRACTATUS");
        assertThat(prompt).as("POSSESSION_ETAT_FAMA doit être dans le prompt questions").contains("POSSESSION_ETAT_FAMA");
        assertThat(prompt).as("POSSESSION_ETAT_CONTINUE doit être dans le prompt questions").contains("POSSESSION_ETAT_CONTINUE");
        assertThat(prompt).as("POSSESSION_ETAT_PAISIBLE doit être dans le prompt questions").contains("POSSESSION_ETAT_PAISIBLE");
        assertThat(prompt).as("POSSESSION_ETAT_NON_EQUIVOQUE doit être dans le prompt questions").contains("POSSESSION_ETAT_NON_EQUIVOQUE");
        // Adoption
        assertThat(prompt).as("ADOPTION_FORME doit être dans le prompt questions").contains("ADOPTION_FORME");
        assertThat(prompt).as("ADOPTION_PUPILLE_ETAT doit être dans le prompt questions").contains("ADOPTION_PUPILLE_ETAT");
        assertThat(prompt).as("ADOPTION_ADOPTANT_MARIE doit être dans le prompt questions").contains("ADOPTION_ADOPTANT_MARIE");
        assertThat(prompt).as("ADOPTION_AGE_ADOPTANT doit être dans le prompt questions").contains("ADOPTION_AGE_ADOPTANT");
        assertThat(prompt).as("ADOPTION_AGE_ADOPTE doit être dans le prompt questions").contains("ADOPTION_AGE_ADOPTE");
        // Réserve héréditaire + rapport à succession
        assertThat(prompt).as("RESERVE_CONJOINT_SURVIVANT doit être dans le prompt questions").contains("RESERVE_CONJOINT_SURVIVANT");
        assertThat(prompt).as("RAPPORT_QUALITE_HERITIER doit être dans le prompt questions").contains("RAPPORT_QUALITE_HERITIER");
        assertThat(prompt).as("RAPPORT_DONATION_NOMINALE doit être dans le prompt questions").contains("RAPPORT_DONATION_NOMINALE");
        assertThat(prompt).as("RAPPORT_VALEUR_PARTAGE doit être dans le prompt questions").contains("RAPPORT_VALEUR_PARTAGE");
        assertThat(prompt).as("RAPPORT_DATE_DONATION doit être dans le prompt questions").contains("RAPPORT_DATE_DONATION");
        assertThat(prompt).as("Annotation FRANCE UNIQUEMENT requise pour codes filiation").contains("FRANCE UNIQUEMENT");
        // Non-régression SF-250-09
        assertThat(prompt).contains("TESTAMENT_FORME");
        assertThat(prompt).contains("FA25_DATE_CERTIFICAT");
        assertThat(prompt).contains("FA06_MODE_GARDE");
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
