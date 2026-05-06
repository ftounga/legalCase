package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-196 SF-196-01 — UT du service d'alignement (matérialisation +
 * propagation pieces auto + lecture).
 *
 * <p>Pattern miroir {@link PieceManquanteAlignmentServiceTest} (F-194).
 * Différence F-196 : modifie le JSON {@code analysis_result.pieces_manquantes}
 * (à la manière de F-192) pour injecter les pièces déduites.</p>
 */
class AiQuestionAlignmentServiceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AiQuestionRepository aiQuestionRepository;
    private AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private CaseAnalysisRepository caseAnalysisRepository;
    private CaseFileRepository caseFileRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private CurrentUserResolver currentUserResolver;

    private AiQuestionAlignmentService service;

    @BeforeEach
    void setUp() {
        aiQuestionRepository = mock(AiQuestionRepository.class);
        aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);
        caseAnalysisRepository = mock(CaseAnalysisRepository.class);
        caseFileRepository = mock(CaseFileRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        currentUserResolver = mock(CurrentUserResolver.class);

        service = new AiQuestionAlignmentService(
                aiQuestionRepository, aiQuestionAnswerRepository, caseAnalysisRepository,
                caseFileRepository, workspaceMemberRepository, currentUserResolver);
    }

    // ======================================================================
    //  materializeForAnalysis
    // ======================================================================

    @Test
    void materializeForAnalysis_noQuestions_persistsEmptyAlignment() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        assertThat(saved.getValue().getAiQuestionsAlignmentJson()).isEqualTo("[]");
    }

    @Test
    void materializeForAnalysis_questionWithPieceAndOuiAnswer_addsPieceObtenue() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        AiQuestion q = newQuestion("Avez-vous reçu la lettre de licenciement ?");
        AiQuestionAnswer answer = newAnswer("oui");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        service.materializeForAnalysis(analysis);

        // 2 saves : 1 pour ai_questions_alignment_json, 1 pour analysis_result modifié
        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(saved.capture());
        CaseAnalysis last = saved.getValue();

        assertThat(last.getAiQuestionsAlignmentJson())
                .contains("\"statutDeduction\":\"PIECE_OBTENUE\"")
                .contains("\"pieceLibelleDeduit\":\"Lettre de licenciement\"")
                .contains("\"answerText\":\"oui\"");

        // Le JSON analysis_result a été enrichi avec une nouvelle pièce
        assertThat(last.getAnalysisResult())
                .contains("\"texte\":\"Lettre de licenciement\"")
                .contains("\"source\":\"QUESTION_REPONDUE_OUI\"");
    }

    @Test
    void materializeForAnalysis_questionWithPieceAndNonAnswer_addsPieceManquante() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        AiQuestion q = newQuestion("Avez-vous le contrat de travail ?");
        AiQuestionAnswer answer = newAnswer("Non");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(saved.capture());
        CaseAnalysis last = saved.getValue();

        assertThat(last.getAiQuestionsAlignmentJson())
                .contains("\"statutDeduction\":\"PIECE_MANQUANTE\"")
                .contains("\"pieceLibelleDeduit\":\"Contrat de travail\"");

        assertThat(last.getAnalysisResult())
                .contains("\"texte\":\"Contrat de travail\"")
                .contains("\"source\":\"QUESTION_REPONDUE_NON\"");
    }

    @Test
    void materializeForAnalysis_questionWithoutAnswer_infoOnlyNoPieceCreated() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        AiQuestion q = newQuestion("Avez-vous le contrat de travail ?");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.empty());

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        CaseAnalysis last = saved.getValue();

        assertThat(last.getAiQuestionsAlignmentJson())
                .contains("\"statutDeduction\":\"INFO_ONLY\"")
                .contains("\"pieceLibelleDeduit\":\"Contrat de travail\"");

        // Pas de pièce ajoutée car réponse non oui/non
        assertThat(last.getAnalysisResult()).isEqualTo("{\"pieces_manquantes\":[]}");
    }

    @Test
    void materializeForAnalysis_questionNoKeyword_infoOnly() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        AiQuestion q = newQuestion("Quelle est la date de l'incident ?");
        AiQuestionAnswer answer = newAnswer("Le 12 mars 2024");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, times(1)).save(saved.capture());
        CaseAnalysis last = saved.getValue();

        // Pas de pièce déduite, statut INFO_ONLY
        assertThat(last.getAiQuestionsAlignmentJson())
                .contains("\"statutDeduction\":\"INFO_ONLY\"");
        assertThat(last.getAiQuestionsAlignmentJson())
                .doesNotContain("\"pieceLibelleDeduit\"");
        assertThat(last.getAnalysisResult()).isEqualTo("{\"pieces_manquantes\":[]}");
    }

    @Test
    void materializeForAnalysis_pieceAlreadyPresent_idempotent() {
        // L'IA a déjà mentionné la pièce → la propagation auto ne doit pas
        // créer de doublon.
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"Lettre de licenciement\"}]}");

        AiQuestion q = newQuestion("Avez-vous reçu la lettre de licenciement ?");
        AiQuestionAnswer answer = newAnswer("non");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(saved.capture());
        CaseAnalysis last = saved.getValue();

        // Une seule occurrence dans pieces_manquantes
        long count = last.getAnalysisResult().split("\"texte\":\"Lettre de licenciement\"", -1).length - 1;
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void materializeForAnalysis_dedupOnNormalizedLibelle_caseInsensitive() {
        // L'IA a mentionné "lettre de licenciement" en minuscule, le mapping
        // produit "Lettre de licenciement" — ne pas dupliquer.
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult(
                "{\"pieces_manquantes\":[{\"texte\":\"  LETTRE de Licenciement  \"}]}");

        AiQuestion q = newQuestion("Avez-vous reçu la lettre de licenciement ?");
        AiQuestionAnswer answer = newAnswer("non");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        service.materializeForAnalysis(analysis);

        ArgumentCaptor<CaseAnalysis> saved = ArgumentCaptor.forClass(CaseAnalysis.class);
        verify(caseAnalysisRepository, atLeastOnce()).save(saved.capture());
        CaseAnalysis last = saved.getValue();

        // analysis_result n'a PAS reçu une 2e entrée (idempotence sur libellé normalisé)
        // Le résultat doit toujours contenir UNE seule entrée pieces_manquantes.
        long count = last.getAnalysisResult().split("\"texte\"", -1).length - 1;
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void materializeForAnalysis_doesNotMutateAiQuestionsTables_F94Strict() {
        // Cohérence F-94 STRICTE : aucune modification ai_questions / ai_question_answers
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        AiQuestion q = newQuestion("Avez-vous reçu la lettre de licenciement ?");
        AiQuestionAnswer answer = newAnswer("oui");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        service.materializeForAnalysis(analysis);

        verify(aiQuestionRepository, never()).save(any());
        verify(aiQuestionAnswerRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_repoThrows_failsOpen() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any()))
                .thenThrow(new RuntimeException("DB down"));

        // Ne doit pas lever — fail-open
        service.materializeForAnalysis(analysis);

        // analysis_result intact
        assertThat(analysis.getAnalysisResult()).isEqualTo("{\"pieces_manquantes\":[]}");
        verify(caseAnalysisRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_nullAnalysis_noOp() {
        service.materializeForAnalysis(null);
        verify(caseAnalysisRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_nullCaseFile_noOp() {
        CaseAnalysis analysis = new CaseAnalysis();
        try {
            Field idField = CaseAnalysis.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(analysis, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Pas de caseFile défini
        service.materializeForAnalysis(analysis);
        verify(caseAnalysisRepository, never()).save(any());
    }

    @Test
    void materializeForAnalysis_runTwice_pieceNotDuplicated() {
        CaseAnalysis analysis = newAnalysis();
        analysis.setAnalysisResult("{\"pieces_manquantes\":[]}");

        AiQuestion q = newQuestion("Avez-vous le contrat de travail ?");
        AiQuestionAnswer answer = newAnswer("non");
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        // 1er run
        service.materializeForAnalysis(analysis);
        // 2e run : même analyse, le JSON pieces_manquantes contient déjà la pièce
        service.materializeForAnalysis(analysis);

        // pieces_manquantes ne contient toujours qu'UNE entrée "Contrat de travail"
        long count = analysis.getAnalysisResult().split("\"texte\":\"Contrat de travail\"", -1).length - 1;
        assertThat(count).isEqualTo(1L);
    }

    // ======================================================================
    //  deserializeAlignment
    // ======================================================================

    @Test
    void deserializeAlignment_validJson_returnsList() throws Exception {
        List<AiQuestionAlignment> list = List.of(
                new AiQuestionAlignment(UUID.randomUUID(), "oui",
                        "Lettre de licenciement", "PIECE_OBTENUE"),
                new AiQuestionAlignment(UUID.randomUUID(), null, null, "INFO_ONLY"));
        String json = MAPPER.writeValueAsString(list);

        List<AiQuestionAlignment> got = service.deserializeAlignment(json);

        assertThat(got).hasSize(2);
        assertThat(got.get(0).statutDeduction()).isEqualTo("PIECE_OBTENUE");
        assertThat(got.get(1).statutDeduction()).isEqualTo("INFO_ONLY");
    }

    @Test
    void deserializeAlignment_nullOrEmpty_returnsEmpty() {
        assertThat(service.deserializeAlignment(null)).isEmpty();
        assertThat(service.deserializeAlignment("")).isEmpty();
        assertThat(service.deserializeAlignment("   ")).isEmpty();
    }

    @Test
    void deserializeAlignment_invalidJson_returnsEmpty() {
        assertThat(service.deserializeAlignment("not-json")).isEmpty();
    }

    // ======================================================================
    //  helpers
    // ======================================================================

    private CaseAnalysis newAnalysis() {
        Workspace ws = new Workspace();
        ws.setName("Test");
        CaseFile cf = new CaseFile();
        cf.setWorkspace(ws);
        CaseAnalysis a = new CaseAnalysis();
        a.setCaseFile(cf);
        try {
            Field idField = CaseAnalysis.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(a, UUID.randomUUID());
            Field cfIdField = CaseFile.class.getDeclaredField("id");
            cfIdField.setAccessible(true);
            cfIdField.set(cf, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        a.setCreatedAt(Instant.now());
        a.setUpdatedAt(Instant.now());
        a.setAnalysisStatus(AnalysisStatus.DONE);
        return a;
    }

    private AiQuestion newQuestion(String text) {
        AiQuestion q = new AiQuestion();
        try {
            Field idField = AiQuestion.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(q, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        q.setQuestionText(text);
        q.setOrderIndex(0);
        return q;
    }

    private AiQuestionAnswer newAnswer(String text) {
        AiQuestionAnswer a = new AiQuestionAnswer();
        a.setAnswerText(text);
        a.setCreatedAt(Instant.now());
        return a;
    }
}
