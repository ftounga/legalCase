package fr.ailegalcase.document;

import fr.ailegalcase.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SF-121-01 — tests unitaires sur la détection des motifs d'échec d'extraction.
 *
 * Les cas couverts correspondent aux critères d'acceptation de la mini-spec :
 * - EMPTY_TEXT (texte vide / blanc)
 * - UNSUPPORTED_FORMAT (contentType non géré)
 * - CORRUPTED (exception PDFBox / POI)
 * - EXTRACTION_EXCEPTION (autre exception)
 * - Non-régression : cas nominal DONE + event publié
 * - Garantie : FAILED ne publie pas ExtractionDoneEvent (ChunkingService pas déclenché)
 */
@ExtendWith(MockitoExtension.class)
class ExtractionServiceTest {

    @Mock private DocumentRepository documentRepository;
    @Mock private DocumentExtractionRepository extractionRepository;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ExtractionService service;

    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String STORAGE_KEY = "ws/cf/doc/file.pdf";

    @BeforeEach
    void setUp() {
        service = new ExtractionService(documentRepository, extractionRepository,
                storageService, eventPublisher);
        Document docRef = new Document();
        docRef.setId(DOC_ID);
        lenient().when(documentRepository.getReferenceById(DOC_ID)).thenReturn(docRef);
        lenient().when(extractionRepository.save(any(DocumentExtraction.class)))
                .thenAnswer(inv -> {
                    DocumentExtraction e = inv.getArgument(0);
                    if (e.getId() == null) e.setId(UUID.randomUUID());
                    return e;
                });
    }

    // U-EXT-01 : texte vide (PDF scanné sans OCR) → FAILED + EMPTY_TEXT + pas d'event
    @Test
    void extract_emptyText_marksFailedWithEmptyTextReason() throws IOException {
        // Un "PDF" minimal qui parse sans exception mais sans texte. On simule via un text/plain vide.
        when(storageService.download(STORAGE_KEY)).thenReturn(new byte[0]);

        service.extract(DOC_ID, STORAGE_KEY, "text/plain");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
        assertThat(saved.getExtractedText()).isNull();
        verify(eventPublisher, never()).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-02 : texte blanc seul → FAILED + EMPTY_TEXT
    @Test
    void extract_whitespaceOnly_marksFailedWithEmptyTextReason() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("   \n\t\r\n  ".getBytes());

        service.extract(DOC_ID, STORAGE_KEY, "text/plain");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
        verify(eventPublisher, never()).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-03 : contentType inconnu → FAILED + UNSUPPORTED_FORMAT
    @Test
    void extract_unsupportedContentType_marksFailedWithUnsupportedFormatReason() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("data".getBytes());

        service.extract(DOC_ID, STORAGE_KEY, "application/vnd.oasis.opendocument.text");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.UNSUPPORTED_FORMAT);
        verify(eventPublisher, never()).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-04 : PDF corrompu (PDFBox throws) → FAILED + CORRUPTED
    @Test
    void extract_corruptedPdf_marksFailedWithCorruptedReason() throws IOException {
        // Bytes invalides qui ne sont pas un PDF valide → PDFBox throw
        when(storageService.download(STORAGE_KEY)).thenReturn("not a real pdf".getBytes());

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.CORRUPTED);
        verify(eventPublisher, never()).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-05 : erreur storage (S3 down, etc.) → FAILED + EXTRACTION_EXCEPTION
    @Test
    void extract_storageException_marksFailedWithExtractionExceptionReason() throws IOException {
        when(storageService.download(STORAGE_KEY))
                .thenThrow(new RuntimeException("S3 unavailable"));

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.EXTRACTION_EXCEPTION);
        verify(eventPublisher, never()).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-06 : non-régression — texte non vide → DONE + event publié
    @Test
    void extract_nominalText_marksDoneAndPublishesEvent() throws IOException {
        when(storageService.download(STORAGE_KEY))
                .thenReturn("Hello world, this is a valid text document.".getBytes());

        service.extract(DOC_ID, STORAGE_KEY, "text/plain");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getFailureReason()).isNull();
        assertThat(saved.getExtractedText()).contains("Hello world");
        verify(eventPublisher).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-07 : metadata JSON contient le reason pour FAILED cases (debug visible en DB)
    @Test
    void extract_emptyText_metadataContainsReason() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn(new byte[0]);

        service.extract(DOC_ID, STORAGE_KEY, "text/plain");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionMetadata()).contains("EMPTY_TEXT");
    }

    /** Récupère le DERNIER DocumentExtraction sauvegardé (reflétant l'état final après logique). */
    private DocumentExtraction capturedFinalSave() {
        ArgumentCaptor<DocumentExtraction> captor = ArgumentCaptor.forClass(DocumentExtraction.class);
        verify(extractionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }
}
