package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

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

    private final AiQuestionService service = new AiQuestionService(
            caseAnalysisRepository, caseFileRepository, aiQuestionRepository,
            analysisJobRepository, anthropicService, usageEventService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "self", service);
        when(caseAnalysisRepository.findById(any())).thenAnswer(inv -> Optional.of(new CaseAnalysis()));
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
        when(anthropicService.analyze(any(), any(), anyInt())).thenReturn(
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
        when(anthropicService.analyze(any(), any(), anyInt())).thenThrow(new RuntimeException("API error"));

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
}
