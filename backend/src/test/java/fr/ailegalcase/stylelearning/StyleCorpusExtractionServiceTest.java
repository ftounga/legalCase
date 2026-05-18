package fr.ailegalcase.stylelearning;

import fr.ailegalcase.analysis.AnthropicResult;
import fr.ailegalcase.analysis.AnthropicService;
import fr.ailegalcase.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * F-98 / SF-98-46 — tests unitaires du worker d'extraction de signature de style.
 *
 * <p>Couvre : succès → {@code DONE} + {@code styleSignature} + purge S3 ; échec IA
 * → {@code FAILED} + {@code errorMessage} + purge S3 ; échec extraction (texte vide)
 * → {@code FAILED} + purge S3 ; le texte brut n'est jamais persisté.</p>
 */
class StyleCorpusExtractionServiceTest {

    private final StyleCorpusRepository styleCorpusRepository = mock(StyleCorpusRepository.class);
    private final StorageService storageService = mock(StorageService.class);
    private final AnthropicService anthropicService = mock(AnthropicService.class);
    private final StyleSignaturePromptBuilder promptBuilder = new StyleSignaturePromptBuilder();

    private final StyleCorpusExtractionService service = new StyleCorpusExtractionService(
            styleCorpusRepository, storageService, anthropicService, promptBuilder);

    private static final String STORAGE_KEY = "style-corpus/ws/doc/conclusion.txt";

    @BeforeEach
    void wireSelf() {
        ReflectionTestUtils.setField(service, "self", service);
    }

    private StyleCorpusDocument pendingDocument(UUID id) {
        StyleCorpusDocument doc = new StyleCorpusDocument();
        ReflectionTestUtils.setField(doc, "id", id);
        doc.setOriginalFilename("conclusion.txt");
        doc.setContentType("text/plain");
        doc.setStatus(StyleCorpusDocumentStatus.PENDING);
        doc.setActive(true);
        return doc;
    }

    @Test
    void process_success_marksDoneWithSignatureAndPurgesS3() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.download(STORAGE_KEY))
                .thenReturn("FAITS ET PROCÉDURE — texte de conclusion.".getBytes(StandardCharsets.UTF_8));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Style assertif, phrases courtes.",
                        "claude-sonnet-4-6", 800, 300, "end_turn"));

        service.process(id, STORAGE_KEY);

        assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.DONE);
        assertThat(doc.getStyleSignature()).isEqualTo("Style assertif, phrases courtes.");
        assertThat(doc.getErrorMessage()).isNull();
        // CA3 — fichier S3 purgé après succès.
        verify(storageService).delete(STORAGE_KEY);
    }

    @Test
    void process_aiFailure_marksFailedAndPurgesS3() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.download(STORAGE_KEY))
                .thenReturn("Texte de conclusion.".getBytes(StandardCharsets.UTF_8));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic API indisponible"));

        service.process(id, STORAGE_KEY);

        assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.FAILED);
        assertThat(doc.getErrorMessage()).contains("Anthropic API indisponible");
        assertThat(doc.getStyleSignature()).isNull();
        // CA7 — fichier S3 purgé même en cas d'échec.
        verify(storageService).delete(STORAGE_KEY);
    }

    @Test
    void process_emptyText_marksFailedAndPurgesS3() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.download(STORAGE_KEY)).thenReturn("   ".getBytes(StandardCharsets.UTF_8));

        service.process(id, STORAGE_KEY);

        assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.FAILED);
        assertThat(doc.getStyleSignature()).isNull();
        verify(storageService).delete(STORAGE_KEY);
        // Aucun appel IA quand le texte extrait est vide.
        verify(anthropicService, never()).analyzeWithSystemCache(any(), any(), anyInt());
    }

    @Test
    void process_emptyAiResponse_marksFailedAndPurgesS3() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.download(STORAGE_KEY))
                .thenReturn("Texte.".getBytes(StandardCharsets.UTF_8));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("  ", "claude-sonnet-4-6", 10, 0, "end_turn"));

        service.process(id, STORAGE_KEY);

        assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.FAILED);
        verify(storageService).delete(STORAGE_KEY);
    }

    @Test
    void process_unknownDocument_stillPurgesS3() {
        UUID id = UUID.randomUUID();
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.empty());

        service.process(id, STORAGE_KEY);

        // Ligne introuvable : aucune extraction, mais le fichier S3 est purgé tout de même.
        verify(storageService).delete(STORAGE_KEY);
        verify(storageService, never()).download(any());
    }

    @Test
    void process_alreadyProcessed_stillPurgesS3() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        doc.setStatus(StyleCorpusDocumentStatus.DONE);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));

        service.process(id, STORAGE_KEY);

        verify(storageService).delete(STORAGE_KEY);
        verify(storageService, never()).download(any());
    }

    @Test
    void process_purgeFailure_doesNotMaskMetierStatus() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(storageService.download(STORAGE_KEY))
                .thenReturn("Texte.".getBytes(StandardCharsets.UTF_8));
        when(anthropicService.analyzeWithSystemCache(any(), any(), anyInt()))
                .thenReturn(new AnthropicResult("Style mesuré.", "claude-sonnet-4-6", 5, 5, "end_turn"));
        doThrow(new RuntimeException("S3 down")).when(storageService).delete(STORAGE_KEY);

        // Une erreur de purge ne propage pas — le statut métier reste persisté.
        service.process(id, STORAGE_KEY);

        assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.DONE);
        verify(storageService).delete(STORAGE_KEY);
    }

    @Test
    void extractText_txt_returnsRawContent() throws Exception {
        String text = service.extractText("Conclusion de référence.".getBytes(StandardCharsets.UTF_8),
                "text/plain", "conclusion.txt");
        assertThat(text).isEqualTo("Conclusion de référence.");
    }

    @Test
    void markProcessing_pendingDocument_movesToProcessing() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean started = service.markProcessing(id);

        assertThat(started).isTrue();
        assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.PROCESSING);
    }

    @Test
    void finalize_neverPersistsRawText() {
        UUID id = UUID.randomUUID();
        StyleCorpusDocument doc = pendingDocument(id);
        when(styleCorpusRepository.findById(id)).thenReturn(Optional.of(doc));
        when(styleCorpusRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.finalize(id, "Description de style.", null);

        // Seule la signature subsiste — aucun champ "texte brut" n'existe sur l'entité.
        assertThat(doc.getStyleSignature()).isEqualTo("Description de style.");
        assertThat(doc.getStatus()).isEqualTo(StyleCorpusDocumentStatus.DONE);
    }
}
