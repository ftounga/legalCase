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
import static org.mockito.ArgumentMatchers.anyString;
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
    @Mock private DocumentPieceRepository documentPieceRepository;
    @Mock private StorageService storageService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private OcrService ocrService;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private OcrRunningFlagService ocrRunningFlagService;
    @Mock private fr.ailegalcase.video.VideoFrameExtractor videoFrameExtractor;
    @Mock private fr.ailegalcase.analysis.AnthropicService anthropicService;
    private fr.ailegalcase.video.VideoFrameExtractorProperties videoProps =
            new fr.ailegalcase.video.VideoFrameExtractorProperties();
    private fr.ailegalcase.document.vision.VisionProperties visionProps =
            new fr.ailegalcase.document.vision.VisionProperties();

    private ExtractionService service;

    private static final UUID DOC_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String STORAGE_KEY = "ws/cf/doc/file.pdf";

    @BeforeEach
    void setUp() {
        service = new ExtractionService(documentRepository, extractionRepository, documentPieceRepository,
                storageService, eventPublisher, ocrService, workspaceRepository,
                ocrRunningFlagService, videoFrameExtractor, videoProps, anthropicService, visionProps);
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

    // ========================================================================
    // SF-230-01 — upload natif des images JPG/PNG/HEIC/WebP
    // ========================================================================

    // U-IMG-01 : image/jpeg + OCR success → DONE via Textract direct (pas de PDFTextStripper)
    @Test
    void extract_imageJpeg_ocrSuccess_marksDoneViaTextract() throws IOException {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        when(storageService.download(STORAGE_KEY)).thenReturn(imageBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("Texte image OCR", 1));

        service.extract(DOC_ID, STORAGE_KEY, "image/jpeg");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getExtractedText()).isEqualTo("Texte image OCR");
        assertThat(saved.getFailureReason()).isNull();
        assertThat(saved.getExtractionMetadata()).contains("textract").contains("\"pageCount\":1");
        verify(eventPublisher).publishEvent(any(ExtractionDoneEvent.class));
        // 1 image = 1 page comptée dans le quota OCR (formsMode=false)
        verify(workspaceRepository).incrementOcrUsage(eq(workspaceId), eq(1),
                any(java.time.LocalDate.class), any(java.time.LocalDate.class));
    }

    // U-IMG-02 : image/png → même flux OCR (parité)
    @Test
    void extract_imagePng_ocrSuccess_marksDoneViaTextract() throws IOException {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        when(storageService.download(STORAGE_KEY)).thenReturn(imageBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("PNG OCR text", 1));

        service.extract(DOC_ID, STORAGE_KEY, "image/png");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getExtractedText()).isEqualTo("PNG OCR text");
    }

    // U-IMG-03 : image/heic → même flux OCR (Textract supporte HEIC nativement)
    @Test
    void extract_imageHeic_ocrSuccess_marksDoneViaTextract() throws IOException {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        when(storageService.download(STORAGE_KEY)).thenReturn(imageBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("HEIC OCR text", 1));

        service.extract(DOC_ID, STORAGE_KEY, "image/heic");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getExtractedText()).isEqualTo("HEIC OCR text");
    }

    // U-IMG-04 : image/webp → même flux OCR
    @Test
    void extract_imageWebp_ocrSuccess_marksDoneViaTextract() throws IOException {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        when(storageService.download(STORAGE_KEY)).thenReturn(imageBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("WebP OCR text", 1));

        service.extract(DOC_ID, STORAGE_KEY, "image/webp");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getExtractedText()).isEqualTo("WebP OCR text");
    }

    // U-IMG-05 : image/svg+xml (non supportée) → IllegalArgumentException → FAILED UNSUPPORTED_FORMAT
    @Test
    void extract_imageSvg_marksFailedWithUnsupportedFormatReason() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("<svg/>".getBytes());

        service.extract(DOC_ID, STORAGE_KEY, "image/svg+xml");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.UNSUPPORTED_FORMAT);
        verify(ocrService, never()).tryOcr(any(), any(), anyBoolean());
    }

    // U-IMG-06 : extractFromImage appelle OcrService.tryOcr quand isOcrEnabled=true
    @Test
    void extractFromImage_callsOcrServiceTryOcr_whenOcrEnabled() {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        Document docRef = documentRepository.getReferenceById(DOC_ID);
        docRef.setOcrEnabled(true);
        docRef.setOcrFormsMode(false);
        when(ocrService.tryOcr(any(), eq(workspaceId), eq(false))).thenReturn(
                fr.ailegalcase.ocr.OcrResult.success("ocr text", 1));

        fr.ailegalcase.ocr.OcrResult result = service.extractFromImage(imageBytes(), "image/jpeg", docRef);

        assertThat(result.success()).isTrue();
        assertThat(result.text()).isEqualTo("ocr text");
        verify(ocrService).tryOcr(any(), eq(workspaceId), eq(false));
    }

    // U-IMG-07 : extractFromImage rejette explicitement un contentType non-image
    @Test
    void extractFromImage_rejectsNonImageContentType() {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        Document docRef = documentRepository.getReferenceById(DOC_ID);

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> service.extractFromImage(imageBytes(), "application/pdf", docRef));
        verify(ocrService, never()).tryOcr(any(), any(), anyBoolean());
    }

    // U-IMG-08 : pour une image, parseText ne tente AUCUNE extraction texte native
    // (pas de PDFTextStripper, pas de POI). Vérifié indirectement : un blob JPEG
    // mal formé ne déclenche pas d'exception parsing — il passe directement à l'OCR.
    @Test
    void extract_imageJpegMalformed_doesNotInvokeNativeTextExtraction() throws IOException {
        UUID workspaceId = UUID.randomUUID();
        setupDocWithWorkspace(workspaceId);
        when(storageService.download(STORAGE_KEY)).thenReturn("not a real jpeg".getBytes());
        when(ocrService.tryOcr(any(), any(), anyBoolean())).thenReturn(
                fr.ailegalcase.ocr.OcrResult.failure(ExtractionFailureReason.OCR_FAILED));

        service.extract(DOC_ID, STORAGE_KEY, "image/jpeg");

        DocumentExtraction saved = capturedFinalSave();
        // Échec doit être OCR_FAILED (pas CORRUPTED — preuve que PDFBox/POI n'a pas tenté)
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.OCR_FAILED);
        verify(ocrService).tryOcr(any(), any(), anyBoolean());
    }

    // U-IMG-09 : ocrEnabled=false sur une image → skip OCR, FAILED EMPTY_TEXT
    @Test
    void extract_imagePng_ocrDisabled_skipsTextract() throws IOException {
        Document docRef = documentRepository.getReferenceById(DOC_ID);
        docRef.setOcrEnabled(false);
        when(storageService.download(STORAGE_KEY)).thenReturn(imageBytes());

        service.extract(DOC_ID, STORAGE_KEY, "image/png");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
        verify(ocrService, never()).tryOcr(any(), any(), anyBoolean());
    }

    /** Bytes minimalement plausibles pour une image (utilisés comme stub OCR). */
    private byte[] imageBytes() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0, 0x10, 0x4A, 0x46, 0x49, 0x46};
    }

    // ── SF-231-01 : ingestion vidéo MP4 / MOV ────────────────────────────

    // U-EXT-VIDEO-01 : video/mp4 → délègue à VideoFrameExtractor + Vision, persiste DocumentPiece + extracted_text
    @Test
    void extract_videoMp4_callsFfmpegAndVision_persistsDescriptionAndPiece() throws IOException {
        byte[] videoBytes = "fake mp4 bytes".getBytes();
        when(storageService.download(STORAGE_KEY)).thenReturn(videoBytes);
        java.util.List<byte[]> frames = java.util.List.of(
                pngBytes(), pngBytes(), pngBytes(), pngBytes(), pngBytes());
        when(videoFrameExtractor.extract5Frames(any())).thenReturn(frames);
        fr.ailegalcase.analysis.AnthropicResult visionResult =
                new fr.ailegalcase.analysis.AnthropicResult("Scène extérieure, parking de nuit, ...",
                        "claude-haiku-4-5-20251001", 100, 200, "end_turn");
        when(anthropicService.analyzeWithImages(anyString(), anyString(), any(), eq("image/png"),
                anyString(), anyInt())).thenReturn(visionResult);

        service.extract(DOC_ID, STORAGE_KEY, "video/mp4");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        assertThat(saved.getExtractedText()).contains("Scène extérieure");
        verify(anthropicService).analyzeWithImages(anyString(), anyString(), eq(frames),
                eq("image/png"), anyString(), eq(2000));
        // Une DocumentPiece PHOTO doit être persistée avec visualDescription
        ArgumentCaptor<DocumentPiece> pieceCaptor = ArgumentCaptor.forClass(DocumentPiece.class);
        verify(documentPieceRepository).save(pieceCaptor.capture());
        DocumentPiece piece = pieceCaptor.getValue();
        assertThat(piece.getType()).isEqualTo(DocumentPieceType.PHOTO);
        assertThat(piece.getVisualDescription()).contains("Scène extérieure");
        assertThat(piece.getVisionStatus()).isEqualTo(VisionStatus.DONE);
        assertThat(piece.getVisionModel()).isNotNull();
        verify(eventPublisher).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-VIDEO-02 : video/quicktime → même comportement
    @Test
    void extract_videoQuicktime_callsFfmpegAndVision() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("fake mov bytes".getBytes());
        when(videoFrameExtractor.extract5Frames(any())).thenReturn(
                java.util.List.of(pngBytes(), pngBytes(), pngBytes(), pngBytes(), pngBytes()));
        when(anthropicService.analyzeWithImages(anyString(), anyString(), any(), anyString(), anyString(), anyInt()))
                .thenReturn(new fr.ailegalcase.analysis.AnthropicResult("description", "claude-haiku-4-5", 50, 80, "end_turn"));

        service.extract(DOC_ID, STORAGE_KEY, "video/quicktime");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.DONE);
        verify(videoFrameExtractor).extract5Frames(any());
    }

    // U-EXT-VIDEO-03 : ffmpeg timeout → FAILED VIDEO_EXTRACTION_TIMEOUT
    @Test
    void extract_videoMp4_ffmpegTimeout_marksFailedTimeout() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("fake mp4".getBytes());
        when(videoFrameExtractor.extract5Frames(any())).thenThrow(
                new fr.ailegalcase.video.VideoExtractionException(
                        fr.ailegalcase.video.VideoExtractionException.Reason.VIDEO_EXTRACTION_TIMEOUT,
                        "ffmpeg timeout"));

        service.extract(DOC_ID, STORAGE_KEY, "video/mp4");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.VIDEO_EXTRACTION_TIMEOUT);
        verify(eventPublisher, never()).publishEvent(any(ExtractionDoneEvent.class));
    }

    // U-EXT-VIDEO-04 : ffmpeg failed (vidéo corrompue) → FAILED VIDEO_EXTRACTION_FAILED
    @Test
    void extract_videoMp4_ffmpegFailed_marksFailed() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("corrupt mp4".getBytes());
        when(videoFrameExtractor.extract5Frames(any())).thenThrow(
                new fr.ailegalcase.video.VideoExtractionException(
                        fr.ailegalcase.video.VideoExtractionException.Reason.VIDEO_EXTRACTION_FAILED,
                        "ffmpeg exit 1"));

        service.extract(DOC_ID, STORAGE_KEY, "video/mp4");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.VIDEO_EXTRACTION_FAILED);
    }

    // U-EXT-VIDEO-05 : Vision API en erreur → FAILED VISION_API_ERROR
    @Test
    void extract_videoMp4_visionApiError_marksFailedVisionApiError() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("fake mp4".getBytes());
        when(videoFrameExtractor.extract5Frames(any())).thenReturn(
                java.util.List.of(pngBytes(), pngBytes(), pngBytes(), pngBytes(), pngBytes()));
        when(anthropicService.analyzeWithImages(anyString(), anyString(), any(), anyString(), anyString(), anyInt()))
                .thenThrow(new RuntimeException("Anthropic 503"));

        service.extract(DOC_ID, STORAGE_KEY, "video/mp4");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.VISION_API_ERROR);
    }

    // U-EXT-VIDEO-06 : Vision retourne description vide → FAILED VIDEO_EXTRACTION_FAILED
    @Test
    void extract_videoMp4_visionEmptyResponse_marksFailed() throws IOException {
        when(storageService.download(STORAGE_KEY)).thenReturn("fake mp4".getBytes());
        when(videoFrameExtractor.extract5Frames(any())).thenReturn(
                java.util.List.of(pngBytes(), pngBytes(), pngBytes(), pngBytes(), pngBytes()));
        when(anthropicService.analyzeWithImages(anyString(), anyString(), any(), anyString(), anyString(), anyInt()))
                .thenReturn(new fr.ailegalcase.analysis.AnthropicResult("   ", "claude-haiku-4-5", 50, 5, "end_turn"));

        service.extract(DOC_ID, STORAGE_KEY, "video/mp4");

        DocumentExtraction saved = capturedFinalSave();
        assertThat(saved.getExtractionStatus()).isEqualTo(ExtractionStatus.FAILED);
        assertThat(saved.getFailureReason()).isEqualTo(ExtractionFailureReason.VIDEO_EXTRACTION_FAILED);
    }

    /** Faux PNG minimal (header magique). Suffisant pour les tests qui mockent AnthropicService. */
    private byte[] pngBytes() {
        return new byte[] { (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R' };
    }
}
