package fr.ailegalcase.document;

import fr.ailegalcase.ocr.OcrService;
import fr.ailegalcase.storage.StorageService;
import fr.ailegalcase.workspace.WorkspaceRepository;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
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
    @Mock private OcrService ocrService;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private OcrRunningFlagService ocrRunningFlagService;

    private ExtractionService service;

    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String STORAGE_KEY = "ws/cf/doc/file.pdf";

    @BeforeEach
    void setUp() {
        service = new ExtractionService(documentRepository, extractionRepository,
                storageService, eventPublisher, ocrService, workspaceRepository,
                ocrRunningFlagService);
        // SF-122-01 : par défaut, OCR indisponible — les tests existants SF-121-01
        // continuent de valider le comportement "texte vide → FAILED EMPTY_TEXT" sur non-PDF
        // (pour PDF, le fallback OCR est testé séparément dans OcrServiceTest + I-ES-OCR-*).
        lenient().when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.failure(ExtractionFailureReason.EMPTY_TEXT));
        // SF-122-02 : caseFile + workspace par défaut — tous les tests OCR PDF ont besoin
        // de resolve workspaceId via docRef.getCaseFile().getWorkspace().getId().
        fr.ailegalcase.casefile.CaseFile cf = new fr.ailegalcase.casefile.CaseFile();
        fr.ailegalcase.workspace.Workspace ws = new fr.ailegalcase.workspace.Workspace();
        ws.setId(UUID.randomUUID());
        cf.setWorkspace(ws);
        Document docRef = new Document();
        docRef.setId(DOC_ID);
        docRef.setCaseFile(cf);
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

    // --- SF-122-01 : fallback OCR sur PDF à texte vide ---

    // U-EXT-OCR-01 : PDF texte vide + OCR success → DONE via textract, workspace incrémenté
    @Test
    void extract_emptyPdf_ocrSuccess_marksDone() throws IOException {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        when(storageService.download(STORAGE_KEY)).thenReturn(emptyPdfBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("Texte OCR reconstruit", 3));

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getExtractedText()).isEqualTo("Texte OCR reconstruit");
        assertThat(saved.getFailureReason()).isNull();
        assertThat(saved.getExtractionMetadata()).contains("textract").contains("\"pageCount\":3");
        verify(eventPublisher).publishEvent(any(ExtractionDoneEvent.class));
        verify(workspaceRepository).incrementOcrUsage(eq(workspaceId), eq(3), any(java.time.LocalDate.class), any(java.time.LocalDate.class));
    }

    // U-EXT-OCR-02 : PDF texte vide + OCR failure → FAILED avec motif propagé
    @Test
    void extract_emptyPdf_ocrFailure_marksFailed() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn(emptyPdfBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.failure(ExtractionFailureReason.OCR_FAILED));

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.OCR_FAILED);
        verify(eventPublisher, never()).publishEvent(any(ExtractionDoneEvent.class));
        verify(workspaceRepository, never()).incrementOcrUsage(any(), anyInt(), any(), any());
    }

    // U-EXT-OCR-03 : non-PDF texte vide → pas d'appel OCR (SF-121-01 conservé)
    @Test
    void extract_emptyTxt_doesNotCallOcr() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn(new byte[0]);

        service.extract(DOC_ID, STORAGE_KEY, "text/plain");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
        verify(ocrService, never()).tryOcr(any(), any(), anyBoolean());
    }

    // U-EXT-OCR-04 : PDF texte vide + OCR UNSUPPORTED_SIZE → FAILED avec motif OCR_UNSUPPORTED_SIZE
    @Test
    void extract_emptyPdf_ocrUnsupportedSize_marksFailedWithMotif() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn(emptyPdfBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.failure(ExtractionFailureReason.OCR_UNSUPPORTED_SIZE));

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.OCR_UNSUPPORTED_SIZE);
    }

    // U-EXT-OCR-07 : SF-122-07 — doc avec ocrEnabled=false → skip OCR, FAILED EMPTY_TEXT
    @Test
    void extract_emptyPdf_ocrDisabled_skipsTextract() throws IOException {
        Document docRef = documentRepository.getReferenceById(DOC_ID);
        docRef.setOcrEnabled(false);

        when(storageService.download(STORAGE_KEY)).thenReturn(emptyPdfBytes());

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
        // Metadata "internal" seul → retry éligible plus tard (SF-122-06 compatible)
        assertThat(saved.getExtractionMetadata()).contains("\"extractor\":\"internal\"").doesNotContain("textract");
        verify(ocrService, never()).tryOcr(any(), any(), anyBoolean());
        verify(workspaceRepository, never()).incrementOcrUsage(any(), anyInt(), any(), any());
    }

    // U-EXT-OCR-05 : SF-122-03 — doc avec ocrFormsMode=true → incrément OCR × 3
    @Test
    void extract_emptyPdf_formsMode_incrementsWorkspaceByTimesThree() throws IOException {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        // Set formsMode=true sur le docRef — on récupère via documentRepository.getReferenceById
        Document docRef = documentRepository.getReferenceById(DOC_ID);
        docRef.setOcrFormsMode(true);

        when(storageService.download(STORAGE_KEY)).thenReturn(emptyPdfBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("Texte formulaire", 2));

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getExtractionMetadata()).contains("\"formsMode\":true").contains("\"quotaPages\":6");
        // Compteur workspace incrémenté de 2 * 3 = 6
        verify(workspaceRepository).incrementOcrUsage(eq(workspaceId), eq(6), any(java.time.LocalDate.class), any(java.time.LocalDate.class));
    }

    /** PDF minimal valide (1 page, aucun texte) → PDFBox parse OK mais renvoie "" → branche fallback OCR déclenchée. */
    private byte[] emptyPdfBytes() {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {
            doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Reconstruit le stub Document avec un caseFile + workspace pour les tests OCR. */
    private void setupDocWithWorkspace(UUID workspaceId) {
        fr.ailegalcase.casefile.CaseFile cf = new fr.ailegalcase.casefile.CaseFile();
        fr.ailegalcase.workspace.Workspace ws = new fr.ailegalcase.workspace.Workspace();
        ws.setId(workspaceId);
        cf.setWorkspace(ws);
        Document docRef = new Document();
        docRef.setId(DOC_ID);
        docRef.setCaseFile(cf);
        when(documentRepository.getReferenceById(DOC_ID)).thenReturn(docRef);
    }

    /** Récupère le DERNIER DocumentExtraction sauvegardé (reflétant l'état final après logique). */
    private DocumentExtraction capturedFinalSave() {
        ArgumentCaptor<DocumentExtraction> captor = ArgumentCaptor.forClass(DocumentExtraction.class);
        verify(extractionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues().get(captor.getAllValues().size() - 1);
    }

    // SF-144-01 U-01 : flag ocr_running commité via OcrRunningFlagService avant ET après tryOcr
    @Test
    void extract_emptyPdf_setsOcrRunningTrueBeforeAndFalseAfter() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn(emptyPdfBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("ocr text", 2));

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(ocrRunningFlagService, ocrService);
        inOrder.verify(ocrRunningFlagService).markOcrRunning(any(), eq(true));
        inOrder.verify(ocrService).tryOcr(any(), any(), anyBoolean());
        inOrder.verify(ocrRunningFlagService).markOcrRunning(any(), eq(false));
    }

    // SF-144-01 U-01b : flag reset à false même si tryOcr jette une exception
    // (try/finally interne à ExtractionService — l'exception est ensuite attrapée
    // par le try/catch général qui marque FAILED EXTRACTION_EXCEPTION)
    @Test
    void extract_emptyPdf_ocrException_resetsOcrRunningFlag() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn(emptyPdfBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenThrow(new RuntimeException("textract down"));

        service.extract(DOC_ID, STORAGE_KEY, "application/pdf");

        verify(ocrRunningFlagService).markOcrRunning(any(), eq(true));
        verify(ocrRunningFlagService).markOcrRunning(any(), eq(false));
    }
}
