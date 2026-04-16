package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SourceExplanationGeneratorTest {

    private AnthropicService anthropicService;
    private DocumentRepository documentRepository;
    private AiQuestionRepository aiQuestionRepository;
    private AiQuestionAnswerRepository aiQuestionAnswerRepository;
    private ProcedureCheckRepository procedureCheckRepository;
    private SourceExplanationGenerator generator;

    @BeforeEach
    void setUp() {
        anthropicService = mock(AnthropicService.class);
        documentRepository = mock(DocumentRepository.class);
        aiQuestionRepository = mock(AiQuestionRepository.class);
        aiQuestionAnswerRepository = mock(AiQuestionAnswerRepository.class);
        procedureCheckRepository = mock(ProcedureCheckRepository.class);
        generator = new SourceExplanationGenerator(
                anthropicService, new ObjectMapper(), documentRepository,
                aiQuestionRepository, aiQuestionAnswerRepository, procedureCheckRepository);
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
    void generate_validHaikuJson_parsesExplanations() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{\"faits\":[]}");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());

        String haikuResponse = """
                {
                  "explanations": [
                    {"sourceKey": "convention_collective", "sourceType": "ANALYSIS_DETECTION",
                     "label": "Synthèse IA", "sentence": "Convention BTP.", "secondaryText": "IDCC 1596"},
                    {"sourceKey": "date_entree", "sourceType": "DOCUMENT",
                     "label": "contrat.pdf", "sentence": "Date d'entrée.", "anchorDocName": "contrat.pdf"}
                  ]
                }
                """;
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haikuResponse, "haiku", 100, 50));

        List<SourceExplanationData> out = generator.generate(cf, a);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).sourceKey()).isEqualTo("convention_collective");
        assertThat(out.get(0).secondaryText()).isEqualTo("IDCC 1596");
        assertThat(out.get(1).sourceKey()).isEqualTo("date_entree");
    }

    @Test
    void generate_documentAnchorResolved() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{}");
        Document doc = new Document(); doc.setId(UUID.randomUUID()); doc.setOriginalFilename("contrat.pdf");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of(doc));
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());
        String haiku = """
                {"explanations":[{"sourceKey":"salaire","sourceType":"DOCUMENT","label":"contrat.pdf","sentence":"x","anchorDocName":"contrat.pdf"}]}
                """;
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haiku, "haiku", 1, 1));
        List<SourceExplanationData> out = generator.generate(cf, a);
        assertThat(out).hasSize(1);
        assertThat(out.get(0).anchorDocId()).isEqualTo(doc.getId());
    }

    @Test
    void generate_questionAnchorResolvedOnlyIfValid() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{}");
        AiQuestion q = new AiQuestion(); q.setId(UUID.randomUUID()); q.setQuestionText("Q ?");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())).thenReturn(Optional.empty());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());

        UUID fakeId = UUID.randomUUID();
        String haiku = String.format("""
                {"explanations":[
                  {"sourceKey":"k1","sourceType":"QUESTION_AI","label":"Q ?","sentence":"x","anchorQuestionId":"%s"},
                  {"sourceKey":"k2","sourceType":"QUESTION_AI","label":"Q ?","sentence":"x","anchorQuestionId":"%s"}
                ]}
                """, q.getId(), fakeId);
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haiku, "haiku", 1, 1));

        List<SourceExplanationData> out = generator.generate(cf, a);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).anchorQuestionId()).isEqualTo(q.getId());
        assertThat(out.get(1).anchorQuestionId()).isNull();
    }

    @Test
    void generate_f96AnchorResolvedOnlyIfValid() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{}");
        ProcedureCheck c = new ProcedureCheck(); c.setCritereCode("FR_CONVOCATION");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of(c));
        String haiku = """
                {"explanations":[
                  {"sourceKey":"FR_CONVOCATION","sourceType":"CHECKLIST_F96","label":"Convocation","sentence":"x","anchorF96Code":"FR_CONVOCATION"},
                  {"sourceKey":"FR_UNKNOWN","sourceType":"CHECKLIST_F96","label":"Inconnu","sentence":"x","anchorF96Code":"FR_INEXISTANT"}
                ]}
                """;
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haiku, "haiku", 1, 1));
        List<SourceExplanationData> out = generator.generate(cf, a);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).anchorF96Code()).isEqualTo("FR_CONVOCATION");
        assertThat(out.get(1).anchorF96Code()).isNull();
    }

    @Test
    void generate_missingPieceAnchorResolvedWithinBounds() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{\"pieces_manquantes\":[\"Contrat signé\",\"Lettre licenciement\"]}");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());
        String haiku = """
                {"explanations":[
                  {"sourceKey":"pk1","sourceType":"MISSING_PIECE","label":"Contrat","sentence":"x","anchorPieceIndex":0},
                  {"sourceKey":"pk2","sourceType":"MISSING_PIECE","label":"Autre","sentence":"x","anchorPieceIndex":5}
                ]}
                """;
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haiku, "haiku", 1, 1));
        List<SourceExplanationData> out = generator.generate(cf, a);
        assertThat(out).hasSize(2);
        assertThat(out.get(0).anchorPieceIndex()).isEqualTo(0);
        assertThat(out.get(1).anchorPieceIndex()).isNull();
    }

    @Test
    void generate_invalidJson_failsOpenEmptyList() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{}");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("not a json {", "haiku", 1, 1));
        assertThat(generator.generate(cf, a)).isEmpty();
    }

    @Test
    void generate_haikuThrows_failsOpenEmptyList() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{}");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of());
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("network"));
        assertThat(generator.generate(cf, a)).isEmpty();
    }

    @Test
    void generate_userMessageListsAllContextSources() {
        CaseFile cf = new CaseFile(); cf.setId(UUID.randomUUID());
        CaseAnalysis a = analysisWithJson("{\"pieces_manquantes\":[\"Contrat\"]}");
        Document d = new Document(); d.setId(UUID.randomUUID()); d.setOriginalFilename("a.pdf");
        AiQuestion q = new AiQuestion(); q.setId(UUID.randomUUID()); q.setQuestionText("Ancienneté ?");
        ProcedureCheck c = new ProcedureCheck(); c.setCritereCode("FR_CONVOCATION");
        c.setDescription("Convocation"); c.setStatut(ProcedureCheckStatus.NON_COMPLIANT);
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of(d));
        when(aiQuestionRepository.findByCaseAnalysisIdOrderByOrderIndex(a.getId())).thenReturn(List.of(q));
        when(aiQuestionAnswerRepository.findFirstByAiQuestionIdOrderByCreatedAtDesc(q.getId())).thenReturn(Optional.empty());
        when(procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(a.getId())).thenReturn(List.of(c));
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("{\"explanations\":[]}", "haiku", 1, 1));

        generator.generate(cf, a);

        ArgumentCaptor<String> cap = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeFast(any(), cap.capture(), anyInt());
        String msg = cap.getValue();
        assertThat(msg).contains("a.pdf").contains("Ancienneté ?")
                .contains("FR_CONVOCATION").contains("Contrat");
    }
}
