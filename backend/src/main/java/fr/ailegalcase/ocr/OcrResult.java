package fr.ailegalcase.ocr;

import fr.ailegalcase.document.ExtractionFailureReason;

/**
 * Résultat d'une tentative OCR — SF-122-01.
 *
 * @param success      true si texte extrait avec succès
 * @param text         texte OCR (non-null si success, null sinon)
 * @param pageCount    nb de pages traitées (0 si skip / échec avant appel AWS)
 * @param failureMotif motif d'échec (null si success) — aligné sur {@link ExtractionFailureReason}
 *                     pour que {@code ExtractionService} puisse le réutiliser directement.
 */
public record OcrResult(
        boolean success,
        String text,
        int pageCount,
        ExtractionFailureReason failureMotif
) {
    public static OcrResult success(String text, int pageCount) {
        return new OcrResult(true, text, pageCount, null);
    }

    public static OcrResult failure(ExtractionFailureReason motif) {
        return new OcrResult(false, null, 0, motif);
    }
}
