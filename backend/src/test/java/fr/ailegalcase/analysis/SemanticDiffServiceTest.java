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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class SemanticDiffServiceTest {

    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final AiQuestionRepository aiQuestionRepository = mock(AiQuestionRepository.class);
    private final AiQuestionAnswerRepository aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);
    private final AnalysisQaSnapshotService analysisQaSnapshotService = mock(AnalysisQaSnapshotService.class);

    private final SemanticDiffService service = new SemanticDiffService(
            anthropicService, aiQuestionRepository, aiQuestionAnswerRepository, analysisQaSnapshotService);

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

    // ─── Parallel Haiku diff ─────────────────────────────────────────────────

    @Test
    void diff_launches5ParallelCalls() throws Exception {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of("A")));
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), analysisJson(List.of("B")));

        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "[{\"text\":\"B\",\"state\":\"added\",\"reason\":\"Nouvelle pièce\"}]",
                        "claude-haiku-4-5", 100, 50));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of());

        service.diff(from, to, List.of(), List.of());

        // 4 sections texte + 1 timeline = 5 appels
        verify(anthropicService, times(5)).analyzeFast(any(), any(), anyInt());
    }

    @Test
    void diff_haiku_nominal_parsesAllStates() throws Exception {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of("A", "B")));
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), analysisJson(List.of("B enrichi", "C")));

        String faitJson = """
                [
                  {"text":"A","state":"removed","reason":"Document retiré"},
                  {"text":"B enrichi","state":"enriched","reason":"Enrichi Q1"},
                  {"text":"C","state":"added","reason":"Nouvelle pièce"}
                ]
                """;
        // défaut : tableau vide pour toutes les sections
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("[]", "claude-haiku-4-5", 10, 5));
        // override pour la section faits uniquement
        when(anthropicService.analyzeFast(eq(SemanticDiffService.SECTION_SYSTEM_PROMPT), contains("faits"), anyInt()))
                .thenReturn(new AnthropicResult(faitJson, "claude-haiku-4-5", 100, 50));
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
    void diff_oneSection_failure_otherSectionsSemanticFallbackExact() throws Exception {
        // analysis avec faits ET risques remplis
        String jsonWithRisques = """
                {"faits":["A"],"points_juridiques":[],"risques":["RisqueX"],"questions_ouvertes":[],"timeline":[]}
                """;
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), jsonWithRisques);
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), jsonWithRisques.replace("\"A\"", "\"B\"").replace("\"RisqueX\"", "\"RisqueY\""));

        // défaut : item B ajouté avec reason pour toutes les sections
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(
                        "[{\"text\":\"B\",\"state\":\"added\",\"reason\":\"Nouvelle pièce\"}]",
                        "claude-haiku-4-5", 50, 20));
        // risques échoue
        when(anthropicService.analyzeFast(eq(SemanticDiffService.SECTION_SYSTEM_PROMPT), contains("risques"), anyInt()))
                .thenThrow(new RuntimeException("API error on risques"));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of());

        AnalysisDiffResponse result = service.diff(from, to, List.of(), List.of());

        // faits → sémantique : B ajouté avec reason
        assertThat(result.faits().added()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("B");
        assertThat(result.faits().added().get(0).reason()).isEqualTo("Nouvelle pièce");
        // risques → fallback exact : RisqueX supprimé sans reason
        assertThat(result.risques().removed()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("RisqueX");
        assertThat(result.risques().removed().get(0).reason()).isNull();
    }

    @Test
    void diff_allSections_failure_globalFallback() throws Exception {
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
    void diff_haiku_invalidJson_sectionFallsBackToExact() throws Exception {
        CaseAnalysis from = analysisWithId(UUID.randomUUID(), analysisJson(List.of("A")));
        CaseAnalysis to = analysisWithId(UUID.randomUUID(), analysisJson(List.of("B")));

        // défaut : tableau vide
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("[]", "haiku", 10, 5));
        // faits retourne du JSON invalide
        when(anthropicService.analyzeFast(eq(SemanticDiffService.SECTION_SYSTEM_PROMPT), contains("faits"), anyInt()))
                .thenReturn(new AnthropicResult("NOT VALID JSON {{", "haiku", 10, 5));
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(any())).thenReturn(List.of());

        AnalysisDiffResponse result = service.diff(from, to, List.of(), List.of());

        // faits → fallback exact
        assertThat(result.faits().added()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("B");
        assertThat(result.faits().removed()).extracting(AnalysisDiffResponse.DiffItem::text).containsExactly("A");
    }

    @Test
    void buildContext_enrichedType_includesQR_fallbackToCurrent() {
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

        // snapshot absent → fallback to current Q&A
        when(analysisQaSnapshotService.buildQaContext(toEnriched.getId())).thenReturn(Optional.empty());
        when(aiQuestionRepository.findByCaseFileIdOrderByOrderIndex(toEnriched.getCaseFile().getId()))
                .thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId()))
                .thenReturn(Optional.of(answer));

        String context = service.buildContext(List.of(), List.of(), AnalysisType.ENRICHED,
                toEnriched.getId(), toEnriched.getCaseFile().getId());

        assertThat(context).contains("Q&R de l'avocat");
        assertThat(context).contains("Quel est le motif ?");
        assertThat(context).contains("Motif économique");
    }

    @Test
    void buildContext_enrichedType_usesSnapshotWhenAvailable() {
        CaseAnalysis toEnriched = enrichedAnalysis(UUID.randomUUID(), analysisJson(List.of()));

        when(analysisQaSnapshotService.buildQaContext(toEnriched.getId()))
                .thenReturn(Optional.of("Q1 : Quel est le motif ?\nR1 : Motif économique\n"));

        String context = service.buildContext(List.of(), List.of(), AnalysisType.ENRICHED,
                toEnriched.getId(), toEnriched.getCaseFile().getId());

        assertThat(context).contains("Quel est le motif ?");
        assertThat(context).contains("Motif économique");
        verifyNoInteractions(aiQuestionRepository);
    }

    @Test
    void buildContext_standardType_noQR() {
        String context = service.buildContext(List.of(), List.of(), AnalysisType.STANDARD,
                UUID.randomUUID(), UUID.randomUUID());
        assertThat(context).doesNotContain("Q&R de l'avocat");
    }

    @Test
    void buildContext_includesDocumentDiff() {
        AnalysisDocument addedDoc = new AnalysisDocument();
        addedDoc.setDocumentName("contrat.pdf");
        AnalysisDocument removedDoc = new AnalysisDocument();
        removedDoc.setDocumentName("avenant.pdf");

        String context = service.buildContext(List.of(removedDoc), List.of(addedDoc),
                AnalysisType.STANDARD, UUID.randomUUID(), UUID.randomUUID());

        assertThat(context).contains("contrat.pdf");
        assertThat(context).contains("avenant.pdf");
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
