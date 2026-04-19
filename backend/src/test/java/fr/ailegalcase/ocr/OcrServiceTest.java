package fr.ailegalcase.ocr;

import fr.ailegalcase.billing.PlanLimitService;
import fr.ailegalcase.document.ExtractionFailureReason;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentRequest;
import software.amazon.awssdk.services.textract.model.AnalyzeDocumentResponse;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.BlockType;
import software.amazon.awssdk.services.textract.model.UnsupportedDocumentException;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * SF-122-01 — tests unitaires OcrService.
 *
 * Couvre les 6 cas critiques de la mini-spec (U-OCR-01-01 à 06) : succès,
 * taille/pages > limite, Textract exception, 0 blocks, toggle désactivé.
 */
class OcrServiceTest {

    private static final OcrProperties PROPS_ENABLED = new OcrProperties(true, "eu-west-3", 5, 11);
    private static final OcrProperties PROPS_DISABLED = new OcrProperties(false, "eu-west-3", 5, 11);

    // U-OCR-01-01 : succès sync — blocks LINE concaténés, pageCount dérivé
    @Test
    void tryOcr_success_returnsTextAndPageCount() {
        TextractClient client = mock(TextractClient.class);
        AnalyzeDocumentResponse response = AnalyzeDocumentResponse.builder()
                .blocks(
                        Block.builder().blockType(BlockType.PAGE).page(1).build(),
                        Block.builder().blockType(BlockType.LINE).text("Bonjour").page(1).build(),
                        Block.builder().blockType(BlockType.LINE).text("le monde").page(1).build(),
                        Block.builder().blockType(BlockType.WORD).text("Bonjour").page(1).build()
                )
                .build();
        when(client.analyzeDocument(any(AnalyzeDocumentRequest.class))).thenReturn(response);

        OcrService svc = new OcrService(Optional.of(client), PROPS_ENABLED, noQuotaBlock());
        OcrResult result = svc.tryOcr(validPdfBytes(), UUID.randomUUID());

        assertThat(result.success()).isTrue();
        assertThat(result.text()).isEqualTo("Bonjour\nle monde");
        assertThat(result.pageCount()).isEqualTo(1);
    }

    // U-OCR-01-02 : doc > 5 Mo → pas d'appel Textract, motif OCR_UNSUPPORTED_SIZE
    @Test
    void tryOcr_tooLarge_skipsTextract() {
        TextractClient client = mock(TextractClient.class);
        byte[] big = new byte[6 * 1024 * 1024]; // 6 Mo

        OcrService svc = new OcrService(Optional.of(client), PROPS_ENABLED, noQuotaBlock());
        OcrResult result = svc.tryOcr(big, UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.failureMotif()).isEqualTo(ExtractionFailureReason.OCR_UNSUPPORTED_SIZE);
        verify(client, never()).analyzeDocument(any(AnalyzeDocumentRequest.class));
    }

    // U-OCR-01-03 : PDF avec > 11 pages détecté via PDFBox → OCR_UNSUPPORTED_SIZE
    @Test
    void tryOcr_tooManyPages_skipsTextract() {
        TextractClient client = mock(TextractClient.class);
        OcrService svc = new OcrService(Optional.of(client), new OcrProperties(true, "eu-west-3", 5, 11), noQuotaBlock());

        OcrResult result = svc.tryOcr(multiPagePdfBytes(12), UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.failureMotif()).isEqualTo(ExtractionFailureReason.OCR_UNSUPPORTED_SIZE);
        verify(client, never()).analyzeDocument(any(AnalyzeDocumentRequest.class));
    }

    // U-OCR-01-04 : Textract lève exception → motif OCR_FAILED
    @Test
    void tryOcr_textractThrows_returnsOcrFailed() {
        TextractClient client = mock(TextractClient.class);
        when(client.analyzeDocument(any(AnalyzeDocumentRequest.class)))
                .thenThrow(UnsupportedDocumentException.builder().message("bad format").build());

        OcrService svc = new OcrService(Optional.of(client), PROPS_ENABLED, noQuotaBlock());
        OcrResult result = svc.tryOcr(validPdfBytes(), UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.failureMotif()).isEqualTo(ExtractionFailureReason.OCR_FAILED);
    }

    // U-OCR-01-05 : Textract renvoie 0 blocks LINE → motif EMPTY_TEXT (OCR n'a rien trouvé)
    @Test
    void tryOcr_emptyBlocks_returnsEmptyText() {
        TextractClient client = mock(TextractClient.class);
        AnalyzeDocumentResponse response = AnalyzeDocumentResponse.builder().blocks(List.of()).build();
        when(client.analyzeDocument(any(AnalyzeDocumentRequest.class))).thenReturn(response);

        OcrService svc = new OcrService(Optional.of(client), PROPS_ENABLED, noQuotaBlock());
        OcrResult result = svc.tryOcr(validPdfBytes(), UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.failureMotif()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
    }

    // U-OCR-01-06 : textract.enabled=false → skip immédiat, motif EMPTY_TEXT, aucun appel SDK
    @Test
    void tryOcr_disabled_shortCircuits() {
        TextractClient client = mock(TextractClient.class);
        OcrService svc = new OcrService(Optional.of(client), PROPS_DISABLED, noQuotaBlock());

        OcrResult result = svc.tryOcr(validPdfBytes(), UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.failureMotif()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
        verify(client, never()).analyzeDocument(any(AnalyzeDocumentRequest.class));
    }

    // U-OCR-01-07 : client bean absent (textract.enabled=true mais pas de bean TextractClient) → skip
    @Test
    void tryOcr_noClientBean_shortCircuits() {
        OcrService svc = new OcrService(Optional.empty(), PROPS_ENABLED, noQuotaBlock());

        OcrResult result = svc.tryOcr(validPdfBytes(), UUID.randomUUID());

        assertThat(result.success()).isFalse();
        assertThat(result.failureMotif()).isEqualTo(ExtractionFailureReason.EMPTY_TEXT);
    }

    // SF-122-02 : gate quota dépassé → OCR_QUOTA_EXCEEDED, pas d'appel Textract
    @Test
    void tryOcr_quotaExceeded_skipsTextract() {
        TextractClient client = mock(TextractClient.class);
        PlanLimitService pls = mock(PlanLimitService.class);
        UUID workspaceId = UUID.randomUUID();
        when(pls.isOcrQuotaExceeded(eq(workspaceId), anyInt())).thenReturn(true);

        OcrService svc = new OcrService(Optional.of(client), PROPS_ENABLED, pls);
        OcrResult result = svc.tryOcr(validPdfBytes(), workspaceId);

        assertThat(result.success()).isFalse();
        assertThat(result.failureMotif()).isEqualTo(ExtractionFailureReason.OCR_QUOTA_EXCEEDED);
        verify(client, never()).analyzeDocument(any(AnalyzeDocumentRequest.class));
    }

    // SF-122-02 : gate quota OK → appel Textract normal
    @Test
    void tryOcr_quotaOk_callsTextract() {
        TextractClient client = mock(TextractClient.class);
        PlanLimitService pls = mock(PlanLimitService.class);
        when(pls.isOcrQuotaExceeded(any(), anyInt())).thenReturn(false);
        AnalyzeDocumentResponse response = AnalyzeDocumentResponse.builder()
                .blocks(Block.builder().blockType(BlockType.LINE).text("OK").page(1).build())
                .build();
        when(client.analyzeDocument(any(AnalyzeDocumentRequest.class))).thenReturn(response);

        OcrService svc = new OcrService(Optional.of(client), PROPS_ENABLED, pls);
        OcrResult result = svc.tryOcr(validPdfBytes(), UUID.randomUUID());

        assertThat(result.success()).isTrue();
        assertThat(result.text()).isEqualTo("OK");
    }

    // --- helpers ---

    /** PlanLimitService stub qui n'empêche jamais l'OCR — isOcrQuotaExceeded toujours false. */
    private PlanLimitService noQuotaBlock() {
        PlanLimitService pls = mock(PlanLimitService.class);
        lenient().when(pls.isOcrQuotaExceeded(any(), anyInt())).thenReturn(false);
        return pls;
    }

    /** Génère un mini-PDF valide à 1 page (PDFBox). */
    private byte[] validPdfBytes() {
        return multiPagePdfBytes(1);
    }

    /** Génère un PDF vide avec {@code n} pages, via PDFBox. */
    private byte[] multiPagePdfBytes(int n) {
        try (org.apache.pdfbox.pdmodel.PDDocument doc = new org.apache.pdfbox.pdmodel.PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            for (int i = 0; i < n; i++) {
                doc.addPage(new org.apache.pdfbox.pdmodel.PDPage());
            }
            doc.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
