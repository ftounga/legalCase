package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.DocumentExtractionRepository;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.document.ExtractionStatus;
import fr.ailegalcase.workspace.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * F-179 SF-179-01 — tests unitaires de {@link JurisprudenceVerificationService}.
 */
class JurisprudenceVerificationServiceTest {

    private final DocumentRepository documentRepository = mock(DocumentRepository.class);
    private final DocumentExtractionRepository documentExtractionRepository =
            mock(DocumentExtractionRepository.class);
    private final JurisprudenceCheckRepository jurisprudenceCheckRepository =
            mock(JurisprudenceCheckRepository.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);

    private final JurisprudenceVerificationService service = new JurisprudenceVerificationService(
            documentRepository, documentExtractionRepository, jurisprudenceCheckRepository, anthropicService);

    private Workspace workspace;
    private CaseFile caseFile;
    private CaseAnalysis analysis;

    @BeforeEach
    void setUp() {
        workspace = new Workspace();
        workspace.setName("ws");

        caseFile = new CaseFile();
        caseFile.setWorkspace(workspace);
        caseFile.setTitle("Dossier test");
        setId(caseFile, "id", UUID.randomUUID());

        analysis = new CaseAnalysis();
        analysis.setCaseFile(caseFile);
        analysis.setVersion(1);
        setId(analysis, "id", UUID.randomUUID());
    }

    @Test
    void verifyForAnalysis_noDocument_persistsNothing() {
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of());

        service.verifyForAnalysis(analysis);

        verify(jurisprudenceCheckRepository, never()).saveAll(any());
        verify(anthropicService, never()).analyzeWithSystemCache(anyString(), anyString(), anyInt());
    }

    @Test
    void verifyForAnalysis_noExtractedText_persistsNothing() {
        Document doc = document("conclusions.pdf");
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(doc));
        when(documentExtractionRepository.findByDocumentIdIn(any()))
                .thenReturn(List.of());

        service.verifyForAnalysis(analysis);

        verify(jurisprudenceCheckRepository, never()).saveAll(any());
        verify(anthropicService, never()).analyzeWithSystemCache(anyString(), anyString(), anyInt());
    }

    @Test
    void verifyForAnalysis_sonnetReturnsChecks_persistsThem() {
        Document doc = document("conclusions_adverses.pdf");
        stubDocumentWithText(doc, "La partie adverse invoque Cass. soc. 25 septembre 2013, n° 12-17.516.");

        String json = """
                {"checks": [
                  {"documentName": "conclusions_adverses.pdf",
                   "reference": "Cass. soc. 25 sept. 2013, n° 12-17.516",
                   "statut": "SUSPECT",
                   "explication": "L'arret existe mais concerne la prescription.",
                   "positionAlleguee": "Fonde la nullite du licenciement.",
                   "claudeConfidence": "HIGH"},
                  {"documentName": "conclusions_adverses.pdf",
                   "reference": "CE 30 juin 2017, n° 398445",
                   "statut": "VERIFIED",
                   "explication": "Existence et position confirmees.",
                   "positionAlleguee": null,
                   "claudeConfidence": "HIGH"}
                ]}""";
        when(anthropicService.analyzeWithSystemCache(anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet", 100, 50));

        service.verifyForAnalysis(analysis);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JurisprudenceCheck>> captor = ArgumentCaptor.forClass(List.class);
        verify(jurisprudenceCheckRepository).saveAll(captor.capture());
        List<JurisprudenceCheck> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved).anySatisfy(c -> {
            assertThat(c.getStatut()).isEqualTo(JurisprudenceCheckStatus.SUSPECT);
            assertThat(c.getPositionAlleguee()).contains("nullite");
            assertThat(c.getWorkspace()).isSameAs(workspace);
            assertThat(c.isWebSearchUsed()).isFalse();
        });
        assertThat(saved).anySatisfy(c ->
                assertThat(c.getStatut()).isEqualTo(JurisprudenceCheckStatus.VERIFIED));
    }

    @Test
    void verifyForAnalysis_unknownStatut_normalizedToUncertain() {
        Document doc = document("conclusions.pdf");
        stubDocumentWithText(doc, "Cass. soc. 1 janvier 2020, n° 19-10.000 selon l'adversaire.");

        String json = """
                {"checks": [
                  {"documentName": "conclusions.pdf",
                   "reference": "Cass. soc. 1 janv. 2020, n° 19-10.000",
                   "statut": "PROBABLY_FAKE",
                   "explication": "Statut inconnu renvoye par le modele.",
                   "claudeConfidence": "LOW"}
                ]}""";
        when(anthropicService.analyzeWithSystemCache(anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult(json, "claude-sonnet", 100, 50));

        service.verifyForAnalysis(analysis);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<JurisprudenceCheck>> captor = ArgumentCaptor.forClass(List.class);
        verify(jurisprudenceCheckRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getStatut())
                .isEqualTo(JurisprudenceCheckStatus.UNCERTAIN);
    }

    @Test
    void verifyForAnalysis_sonnetThrows_failOpenNoPersist() {
        Document doc = document("conclusions.pdf");
        stubDocumentWithText(doc, "Cass. soc. 25 septembre 2013, n° 12-17.516.");
        when(anthropicService.analyzeWithSystemCache(anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic 500"));

        // Ne doit PAS propager l'exception (fail-open).
        service.verifyForAnalysis(analysis);

        verify(jurisprudenceCheckRepository, never()).saveAll(any());
    }

    @Test
    void verifyForAnalysis_invalidJson_failOpenNoPersist() {
        Document doc = document("conclusions.pdf");
        stubDocumentWithText(doc, "Cass. soc. 25 septembre 2013, n° 12-17.516.");
        when(anthropicService.analyzeWithSystemCache(anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("ceci n'est pas du JSON", "claude-sonnet", 10, 5));

        service.verifyForAnalysis(analysis);

        verify(jurisprudenceCheckRepository, never()).saveAll(any());
    }

    @Test
    void verifyForAnalysis_emptyChecksArray_persistsNothing() {
        Document doc = document("conclusions.pdf");
        stubDocumentWithText(doc, "Aucune jurisprudence citee ici.");
        when(anthropicService.analyzeWithSystemCache(anyString(), anyString(), anyInt()))
                .thenReturn(new AnthropicResult("{\"checks\": []}", "claude-sonnet", 10, 5));

        service.verifyForAnalysis(analysis);

        verify(jurisprudenceCheckRepository, never()).saveAll(any());
    }

    // ---- helpers ----

    private void stubDocumentWithText(Document doc, String text) {
        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setDocument(doc);
        extraction.setExtractionStatus(ExtractionStatus.DONE);
        extraction.setExtractedText(text);
        when(documentRepository.findByCaseFile_IdOrderByCreatedAtDesc(any()))
                .thenReturn(List.of(doc));
        when(documentExtractionRepository.findByDocumentIdIn(any()))
                .thenReturn(List.of(extraction));
    }

    private Document document(String filename) {
        Document doc = new Document();
        doc.setCaseFile(caseFile);
        doc.setOriginalFilename(filename);
        setId(doc, "id", UUID.randomUUID());
        return doc;
    }

    private static void setId(Object entity, String field, UUID value) {
        try {
            java.lang.reflect.Field f = entity.getClass().getDeclaredField(field);
            f.setAccessible(true);
            f.set(entity, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
