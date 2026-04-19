package fr.ailegalcase.ocr;

import fr.ailegalcase.document.DocumentExtraction;
import fr.ailegalcase.document.ExtractionFailureReason;
import fr.ailegalcase.document.ExtractionStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SF-122-06 : filtre de pertinence du retry OCR.
 */
class OcrRetryServiceTest {

    // U-OCR-RETRY-01 : doc legacy (OCR jamais tenté) EMPTY_TEXT → éligible
    @Test
    void isRetryWorth_emptyTextLegacy_returnsTrue() {
        DocumentExtraction e = new DocumentExtraction();
        e.setExtractionStatus(ExtractionStatus.FAILED);
        e.setFailureReason(ExtractionFailureReason.EMPTY_TEXT);
        e.setExtractionMetadata("{\"extractor\":\"internal\",\"charCount\":0,\"durationMs\":50,\"reason\":\"EMPTY_TEXT\"}");

        assertThat(OcrRetryService.isRetryWorthAttempt(e)).isTrue();
    }

    // U-OCR-RETRY-02 : doc avec OCR déjà tenté et EMPTY_TEXT → NON éligible (futile)
    @Test
    void isRetryWorth_emptyTextAfterOcr_returnsFalse() {
        DocumentExtraction e = new DocumentExtraction();
        e.setExtractionStatus(ExtractionStatus.FAILED);
        e.setFailureReason(ExtractionFailureReason.EMPTY_TEXT);
        e.setExtractionMetadata("{\"extractor\":\"internal+textract\",\"charCount\":0,\"durationMs\":120,\"reason\":\"EMPTY_TEXT\"}");

        assertThat(OcrRetryService.isRetryWorthAttempt(e)).isFalse();
    }

    // U-OCR-RETRY-03 : OCR_FAILED → toujours éligible (transient AWS possible)
    @Test
    void isRetryWorth_ocrFailed_returnsTrue() {
        DocumentExtraction e = new DocumentExtraction();
        e.setExtractionStatus(ExtractionStatus.FAILED);
        e.setFailureReason(ExtractionFailureReason.OCR_FAILED);
        e.setExtractionMetadata("{\"extractor\":\"internal+textract\",\"error\":\"Timeout\",\"reason\":\"OCR_FAILED\"}");

        assertThat(OcrRetryService.isRetryWorthAttempt(e)).isTrue();
    }

    // U-OCR-RETRY-04 : metadata null → traité comme legacy, EMPTY_TEXT éligible
    @Test
    void isRetryWorth_emptyTextNullMetadata_returnsTrue() {
        DocumentExtraction e = new DocumentExtraction();
        e.setExtractionStatus(ExtractionStatus.FAILED);
        e.setFailureReason(ExtractionFailureReason.EMPTY_TEXT);
        e.setExtractionMetadata(null);

        assertThat(OcrRetryService.isRetryWorthAttempt(e)).isTrue();
    }
}
