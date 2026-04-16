package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SourceExplanationGeneratorTest {

    private AnthropicService anthropicService;
    private DocumentRepository documentRepository;
    private SourceExplanationGenerator generator;

    @BeforeEach
    void setUp() {
        anthropicService = mock(AnthropicService.class);
        documentRepository = mock(DocumentRepository.class);
        generator = new SourceExplanationGenerator(anthropicService, new ObjectMapper(), documentRepository);
    }

    @Test
    void generate_emptyAnalysisJson_returnsEmptyList() {
        CaseFile cf = new CaseFile();
        assertThat(generator.generate(cf, null)).isEmpty();
        assertThat(generator.generate(cf, "")).isEmpty();
        verifyNoInteractions(anthropicService);
    }

    @Test
    void generate_validHaikuJson_parsesExplanations() {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        String haikuResponse = """
                {
                  "explanations": [
                    {
                      "sourceKey": "convention_collective",
                      "sourceType": "ANALYSIS_DETECTION",
                      "label": "Synthèse IA",
                      "sentence": "La convention BTP s'applique au dossier."
                    },
                    {
                      "sourceKey": "date_entree",
                      "sourceType": "DOCUMENT",
                      "label": "contrat.pdf",
                      "sentence": "Date d'entrée relevée dans le contrat.",
                      "anchorDocName": "contrat.pdf"
                    }
                  ]
                }
                """;
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haikuResponse, "claude-haiku", 100, 50));

        List<SourceExplanationData> out = generator.generate(cf, "{\"faits\":[]}");
        assertThat(out).hasSize(2);
        assertThat(out.get(0).sourceKey()).isEqualTo("convention_collective");
        assertThat(out.get(0).anchorDocId()).isNull();
        assertThat(out.get(1).sourceKey()).isEqualTo("date_entree");
    }

    @Test
    void generate_documentAnchorResolvesToDocId() {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        Document doc = new Document();
        doc.setId(UUID.randomUUID());
        doc.setOriginalFilename("contrat.pdf");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of(doc));
        String haiku = """
                {"explanations":[{"sourceKey":"salaire_brut_mensuel","sourceType":"DOCUMENT","label":"contrat.pdf","sentence":"x","anchorDocName":"contrat.pdf"}]}
                """;
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haiku, "haiku", 1, 1));
        List<SourceExplanationData> out = generator.generate(cf, "{}");
        assertThat(out).hasSize(1);
        assertThat(out.get(0).anchorDocId()).isEqualTo(doc.getId());
    }

    @Test
    void generate_invalidJson_failsOpenEmptyList() {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("not a json {", "haiku", 1, 1));
        assertThat(generator.generate(cf, "{}")).isEmpty();
    }

    @Test
    void generate_haikuThrows_failsOpenEmptyList() {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("network"));
        assertThat(generator.generate(cf, "{}")).isEmpty();
    }

    @Test
    void generate_jsonWrappedInMarkdown_stillParses() {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of());
        String haikuMd = "```json\n{\"explanations\":[{\"sourceKey\":\"k\",\"sourceType\":\"ANALYSIS_DETECTION\",\"label\":\"L\"}]}\n```";
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult(haikuMd, "haiku", 1, 1));
        assertThat(generator.generate(cf, "{}")).hasSize(1);
    }

    @Test
    void generate_promptContainsDocumentList() {
        CaseFile cf = new CaseFile();
        cf.setId(UUID.randomUUID());
        Document d1 = new Document(); d1.setId(UUID.randomUUID()); d1.setOriginalFilename("a.pdf");
        Document d2 = new Document(); d2.setId(UUID.randomUUID()); d2.setOriginalFilename("b.pdf");
        when(documentRepository.findByCaseFileOrderByCreatedAtDesc(cf)).thenReturn(List.of(d1, d2));
        when(anthropicService.analyzeFast(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("{\"explanations\":[]}", "haiku", 1, 1));

        generator.generate(cf, "{\"faits\":[]}");

        ArgumentCaptor<String> userMessageCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicService).analyzeFast(any(), userMessageCaptor.capture(), anyInt());
        String msg = userMessageCaptor.getValue();
        assertThat(msg).contains("a.pdf").contains("b.pdf");
    }
}
