package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * SF-IA-03-20 : après migration Option A, le generator extrait directement
 * `source_explanations` du JSON Sonnet (plus d'appel Haiku séparé), puis
 * complète via fallback heuristique.
 */
class SourceExplanationGeneratorTest {

    private AnthropicService anthropicService;
    private DocumentRepository documentRepository;
    private AiQuestionRepository aiQuestionRepository;
    private AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private ProcedureCheckRepository procedureCheckRepository;
    private SourceExplanationFallback fallback;
    private SourceExplanationGenerator generator;

    @BeforeEach
    void setUp() {
        anthropicService = mock(AnthropicService.class);
        documentRepository = mock(DocumentRepository.class);
        aiQuestionRepository = mock(AiQuestionRepository.class);
        aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);
        procedureCheckRepository = mock(ProcedureCheckRepository.class);
        fallback = mock(SourceExplanationFallback.class);
        when(fallback.buildFallbacks(any(), any(), any(), any())).thenReturn(List.of());
        generator = new SourceExplanationGenerator(
                anthropicService, new ObjectMapper(), documentRepository,
                aiQuestionRepository, aiQuestionAnswerRepository, procedureCheckRepository, fallback);
    }

    private CaseAnalysis analysisWithJson(String json) {
        CaseAnalysis a = new CaseAnalysis();
        a.setId(UUID.randomUUID());
        a.setAnalysisResult(json);
        return a;
    }

    @Test
    void generate_emptyAnalysis_returnsEmptyList() {
        CaseFile cf = new CaseFile();
        assertThat(generator.generate(cf, null)).isEmpty();
        assertThat(generator.generate(cf, analysisWithJson(null))).isEmpty();
        assertThat(generator.generate(cf, analysisWithJson(""))).isEmpty();
        verifyNoInteractions(anthropicService);
    }

    @Test
    void generate_sonnetJsonWithSourceExplanations_parsesAll() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        String sonnetJson = """
                {
                  "faits": [],
                  "source_explanations": [
                    {"sourceKey": "convention_collective", "sourceType": "ANALYSIS_DETECTION",
                     "label": "Synthèse du dossier", "sentence": "Convention BTP applicable.",
                     "secondaryText": "IDCC 1596"},
                    {"sourceKey": "date_entree", "sourceType": "DOCUMENT",
                     "label": "contrat.pdf", "sentence": "Date d'entrée relevée.",
                     "anchorDocName": "contrat.pdf"}
                  ]
                }
                """;
        CaseAnalysis a = analysisWithJson(sonnetJson);
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());

        List<SourceExplanationData> out = generator.generate(cf, a);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).sourceKey()).isEqualTo("convention_collective");
        assertThat(out.get(0).secondaryText()).isEqualTo("IDCC 1596");
        assertThat(out.get(1).sourceKey()).isEqualTo("date_entree");
        verifyNoInteractions(anthropicService);
    }

    @Test
    void generate_documentAnchorResolved() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        Document doc = new Document(); doc.setId(UUID.randomUUID()); doc.setOriginalFilename("contrat.pdf");
        String sonnetJson = """
                {"source_explanations":[{"sourceKey":"salaire","sourceType":"DOCUMENT",
                "label":"contrat.pdf","sentence":"x","anchorDocName":"contrat.pdf"}]}
                """;
        CaseAnalysis a = analysisWithJson(sonnetJson);
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of(doc));
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());

        List<SourceExplanationData> out = generator.generate(cf, a);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).anchorDocId()).isEqualTo(doc.getId());
    }

    @Test
    void generate_questionAnchorResolvedOnlyIfValid() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        AiQuestion q = new AiQuestion(); q.setId(UUID.randomUUID()); q.setQuestionText("Q ?");
        UUID fakeId = UUID.randomUUID();
        String sonnetJson = String.format("""
                {"source_explanations":[
                  {"sourceKey":"k1","sourceType":"QUESTION_AI","label":"Q ?","sentence":"x","anchorQuestionId":"%s"},
                  {"sourceKey":"k2","sourceType":"QUESTION_AI","label":"Q ?","sentence":"x","anchorQuestionId":"%s"}
                ]}
                """, q.getId(), fakeId);
        CaseAnalysis a = analysisWithJson(sonnetJson);
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of(q));
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());

        List<SourceExplanationData> out = generator.generate(cf, a);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).anchorQuestionId()).isEqualTo(q.getId());
        assertThat(out.get(1).anchorQuestionId()).isNull();
    }

    @Test
    void generate_f96AnchorResolvedOnlyIfValid() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        ProcedureCheck c = new ProcedureCheck(); c.setCritereCode("FR_CONVOCATION");
        String sonnetJson = """
                {"source_explanations":[
                  {"sourceKey":"FR_CONVOCATION","sourceType":"CHECKLIST_F96","label":"Convocation","sentence":"x","anchorF96Code":"FR_CONVOCATION"},
                  {"sourceKey":"FR_UNKNOWN","sourceType":"CHECKLIST_F96","label":"Inconnu","sentence":"x","anchorF96Code":"FR_INEXISTANT"}
                ]}
                """;
        CaseAnalysis a = analysisWithJson(sonnetJson);
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of(c));

        List<SourceExplanationData> out = generator.generate(cf, a);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).anchorF96Code()).isEqualTo("FR_CONVOCATION");
        assertThat(out.get(1).anchorF96Code()).isNull();
    }

    @Test
    void generate_missingPieceAnchorResolvedWithinBounds() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        String sonnetJson = """
                {"pieces_manquantes":["Contrat","Lettre"],"source_explanations":[
                  {"sourceKey":"pk1","sourceType":"MISSING_PIECE","label":"Contrat","sentence":"x","anchorPieceIndex":0},
                  {"sourceKey":"pk2","sourceType":"MISSING_PIECE","label":"Autre","sentence":"x","anchorPieceIndex":5}
                ]}
                """;
        CaseAnalysis a = analysisWithJson(sonnetJson);
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());

        List<SourceExplanationData> out = generator.generate(cf, a);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).anchorPieceIndex()).isEqualTo(0);
        assertThat(out.get(1).anchorPieceIndex()).isNull();
    }

    @Test
    void generate_invalidJson_failsOpenEmptyList() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("not a json {");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());

        assertThat(generator.generate(cf, a)).isEmpty();
    }

    @Test
    void generate_noSourceExplanationsField_fallbackOnly() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{\"faits\":[]}");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());
        SourceExplanationData fb = new SourceExplanationData("convention_collective", "DOCUMENT",
                "contrat.pdf", null, "Extrait", UUID.randomUUID(), null, null, null, null);
        when(fallback.buildFallbacks(any(), any(), any(), any())).thenReturn(List.of(fb));

        List<SourceExplanationData> out = generator.generate(cf, a);

        assertThat(out).hasSize(1);
        assertThat(out.get(0).sourceKey()).isEqualTo("convention_collective");
    }

    @Test
    void generate_fallbackFillsMissingKeys() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        String sonnetJson = """
                {"source_explanations":[
                  {"sourceKey":"date_entree","sourceType":"ANALYSIS_DETECTION","label":"Synthèse","sentence":"x"}
                ]}
                """;
        CaseAnalysis a = analysisWithJson(sonnetJson);
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());
        SourceExplanationData fb = new SourceExplanationData("convention_collective", "ANALYSIS_DETECTION",
                "Synthèse", null, "BTP", null, null, null, null, null);
        when(fallback.buildFallbacks(any(), any(), any(), any())).thenReturn(List.of(fb));

        List<SourceExplanationData> out = generator.generate(cf, a);

        assertThat(out).hasSize(2);
        assertThat(out.get(0).sourceKey()).isEqualTo("date_entree"); // Sonnet
        assertThat(out.get(1).sourceKey()).isEqualTo("convention_collective"); // Fallback
    }
}
