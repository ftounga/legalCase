package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

class SemanticDiffServiceTest {

    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);

    private final SemanticDiffService service = new SemanticDiffService(
            anthropicService, aiQuestionRepository, aiQuestionAnswerRepository);

    // ─── exactDiff ───────────────────────────────────────────────────────────

    @Test
    void exactDiff_added_removed_unchanged() {
        CaseAnalysis from = analysis(analysisJson(List.of("A", "B")));
        CaseAnalysis to = analysis(analysisJson(List.of("B", "C")));

        AnalysisDiffResponse result = service.exactDiff(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(to), from, to);

        assertThat(result.faits().added()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("C");
        assertThat(result.faits().removed()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("A");
        assertThat(result.faits().unchanged()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("B");
        assertThat(result.faits().enriched()).isEmpty();
    }

    @Test
    void exactDiff_identical_allUnchanged() {
        String json = analysisJson(List.of("A", "B"));
        CaseAnalysis from = analysis(json);
        CaseAnalysis to = analysis(json);

        AnalysisDiffResponse result = service.exactDiff(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(to), from, to);

        assertThat(result.faits().added()).isEmpty();
        assertThat(result.faits().removed()).isEmpty();
        assertThat(result.faits().unchanged()).extracting(AnalysisDiffResponse.DiffItem::text)
                .containsExactlyInAnyOrder("A", "B");
    }

    @Test
    void exactDiff_fromEmpty_allAdded() {
        CaseAnalysis from = analysis(analysisJson(List.of()));
        CaseAnalysis to = analysis(analysisJson(List.of("X", "Y")));

        AnalysisDiffResponse result = service.exactDiff(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(to), from, to);

        assertThat(result.faits().added()).extracting(AnalysisDiffResponse.DiffItem::text)
                .containsExactlyInAnyOrder("X", "Y");
        assertThat(result.faits().removed()).isEmpty();
    }

    @Test
    void exactDiff_toEmpty_allRemoved() {
        CaseAnalysis from = analysis(analysisJson(List.of("X", "Y")));
        CaseAnalysis to = analysis(analysisJson(List.of()));

        AnalysisDiffResponse result = service.exactDiff(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(to), from, to);

        assertThat(result.faits().removed()).extracting(AnalysisDiffResponse.DiffItem::text)
                .containsExactlyInAnyOrder("X", "Y");
        assertThat(result.faits().added()).isEmpty();
    }

    @Test
    void exactDiff_timeline_added_removed_unchanged() {
        CaseAnalysis from = analysis("""
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],
                 "timeline":[{"date":"2024-01-01","evenement":"Embauche"},{"date":"2024-06-01","evenement":"Licenciement"}]}
                """);
        CaseAnalysis to = analysis("""
                {"faits":[],"points_juridiques":[],"risques":[],"questions_ouvertes":[],
                 "timeline":[{"date":"2024-01-01","evenement":"Embauche"},{"date":"2024-07-01","evenement":"Jugement"}]}
                """);

        AnalysisDiffResponse result = service.exactDiff(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(to), from, to);

        assertThat(result.timeline().unchanged())
                .extracting(AnalysisDiffResponse.TimelineDiffItem::evenement).containsExactly("Embauche");
        assertThat(result.timeline().removed())
                .extracting(AnalysisDiffResponse.TimelineDiffItem::evenement).containsExactly("Licenciement");
        assertThat(result.timeline().added())
                .extracting(AnalysisDiffResponse.TimelineDiffItem::evenement).containsExactly("Jugement");
    }

    @Test
    void exactDiff_unchangedItemsHaveNullReason() {
        String json = analysisJson(List.of("A"));
        CaseAnalysis from = analysis(json);
        CaseAnalysis to = analysis(json);

        AnalysisDiffResponse result = service.exactDiff(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(to), from, to);

        assertThat(result.faits().unchanged()).hasSize(1);
        assertThat(result.faits().unchanged().get(0).reason()).isNull();
    }

    // ─── Haiku diff ──────────────────────────────────────────────────────────

    @Test
    void diff_haiku_nominal_parsesAllStates() throws Exception {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of("A", "B")));
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), analysisJson(List.of("B enrichi", "C")));

        String haikuJson = """
                {"faits":[
                  {"text":"A","state":"removed","reason":"Document retiré"},
                  {"text":"B enrichi","state":"enriched","reason":"Enrichi Q1"},
                  {"text":"C","state":"added","reason":"Nouvelle pièce"}
                ],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """;
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haikuJson, "claude-haiku-4-5", 100, 50));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of());

        AnalysisDiffResponse result = service.diff(from, to, List.of(), List.of());

        assertThat(result.faits().removed()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("A");
        assertThat(result.faits().removed().get(0).reason()).isEqualTo("Document retiré");
        assertThat(result.faits().enriched()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("B enrichi");
        assertThat(result.faits().enriched().get(0).reason()).isEqualTo("Enrichi Q1");
        assertThat(result.faits().added()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("C");
        assertThat(result.faits().added().get(0).reason()).isEqualTo("Nouvelle pièce");
    }

    @Test
    void diff_haiku_failure_fallsBackToExactDiff() {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of("A", "B")));
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), analysisJson(List.of("B", "C")));

        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("API error"));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of());

        AnalysisDiffResponse result = service.diff(from, to, List.of(), List.of());

        assertThat(result.faits().added()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("C");
        assertThat(result.faits().removed()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("A");
        assertThat(result.faits().unchanged()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("B");
    }

    @Test
    void diff_haiku_invalidJson_fallsBackToExactDiff() {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of("A")));
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), analysisJson(List.of("B")));

        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("NOT VALID JSON {{", "haiku", 10, 5));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of());

        AnalysisDiffResponse result = service.diff(from, to, List.of(), List.of());

        assertThat(result.faits().added()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("B");
        assertThat(result.faits().removed()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("A");
    }

    @Test
    void buildPrompt_enrichedType_includesQR() {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of()));
        CaseAnalysis toEnriched = enrichedAnalysis(UUID.randomUUID(), analysisJson(List.of()));

        AiQuestion q = new AiQuestion();
        q.setId(UUID.randomUUID());
        q.setQuestionText("Quel est le motif ?");
        q.setStatus("ANSWERED");
        q.setOrderIndex(0);
        q.setCaseFile(toEnriched.getCaseFile());

        AiQuestionAnswer answer = new AiQuestionAnswer();
        answer.setAnswerText("Motif économique");

        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(toEnriched.getCaseFile().getId()))
                .thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        String prompt = service.buildPrompt(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(toEnriched),
                List.of(), List.of(), AnalysisType.ENRICHED, toEnriched.getCaseFile().getId());

        assertThat(prompt).contains("Q&R de l'avocat");
        assertThat(prompt).contains("Quel est le motif ?");
        assertThat(prompt).contains("Motif économique");
    }

    @Test
    void buildPrompt_standardType_includesDocumentDiff() {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of()));
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), analysisJson(List.of()));

        AnalysisDocument addedDoc = new AnalysisDocument();
        addedDoc.setDocumentName("contrat.pdf");
        AnalysisDocument removedDoc = new AnalysisDocument();
        removedDoc.setDocumentName("avenant.pdf");

        String prompt = service.buildPrompt(
                CaseAnalysisResponse.from(from), CaseAnalysisResponse.from(to),
                List.of(removedDoc), List.of(addedDoc), AnalysisType.STANDARD, UUID.randomUUID());

        assertThat(prompt).contains("contrat.pdf");
        assertThat(prompt).contains("avenant.pdf");
        assertThat(prompt).doesNotContain("Q&R de l'avocat");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CaseAnalysis analysis(String json) {
        return analysisWithId(UUID.randomUUID(), json);
    }

    private CaseAnalysis analysisWithId(UUID id, String json) {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());

        CaseAnalysis a = new CaseAnalysis();
        a.setId(id);
        a.setCaseFile(cf);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisType(AnalysisType.STANDARD);
        a.setAnalysisResult(json);
        a.setVersion(1);
        a.setUpdatedAt(Instant.now());
        return a;
    }

    private CaseAnalysis enrichedAnalysis(UUID id, String json) {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());

        CaseAnalysis a = new CaseAnalysis();
        a.setId(id);
        a.setCaseFile(cf);
        a.setAnalysisStatus(AnalysisStatus.DONE);
        a.setAnalysisType(AnalysisType.ENRICHED);
        a.setAnalysisResult(json);
        a.setVersion(2);
        a.setUpdatedAt(Instant.now());
        return a;
    }

    private String analysisJson(List<String> faits) {
        String items = faits.stream()
                .map(f -> "\"" + f + "\"")
                .reduce((a, b) -> a + "," + b)
                .orElse("");
        return """
                {"faits":[%s],"points_juridiques":[],"risques":[],"questions_ouvertes":[],"timeline":[]}
                """.formatted(items);
    }
}
