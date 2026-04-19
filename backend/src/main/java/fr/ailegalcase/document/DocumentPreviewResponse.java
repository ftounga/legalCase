package fr.ailegalcase.document;

import java.time.Instant;

/**
 * SF-127-01 : payload renvoyé par GET /documents/{id}/preview.
 * Permet à l'UI d'afficher le texte extrait (ce que Claude voit) + metadata
 * pour rassurer l'avocat avant de lancer une analyse payante.
 *
 * @param extractionMethod CLASSIC (PDFBox natif) · OCR (Textract) · NONE (pas encore / échec)
 * @param extractedText null si extraction non DONE, sinon tronqué à TEXT_TRUNCATE_LIMIT
 * @param textTruncated true si le texte original dépassait la limite et a été coupé
 * @param failureReason renseigné uniquement si extractionStatus=FAILED
 */
public record DocumentPreviewResponse(
        String fileName,
        String mimeType,
        long fileSize,
        Integer pageCount,
        Instant uploadedAt,
        ExtractionStatus extractionStatus,
        ExtractionMethod extractionMethod,
        String extractedText,
        int charCount,
        boolean textTruncated,
        int ocrPagesUsed,
        ExtractionFailureReason failureReason
) {
    public enum ExtractionMethod { CLASSIC, OCR, NONE }

    /** Limite de caractères renvoyés côté front. 200 000 car ≈ ~100 pages A4 denses. */
    public static final int TEXT_TRUNCATE_LIMIT = 200_000;
}
