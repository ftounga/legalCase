package fr.ailegalcase.ocr;

import fr.ailegalcase.document.ExtractionFailureReason;

/**
 * Résultat d'une tentative OCR — SF-122-01, SF-122-08.
 *
 * @param success      true si texte extrait avec succès
 * @param text         texte OCR (non-null si success, null sinon)
 * @param pageCount    nb de pages traitées (0 si skip / échec avant appel AWS)
 * @param failureMotif motif d'échec (null si success)
 * @param rasterized   SF-122-08 : true si le texte a été extrait via rasterisation
 *                     PDF → PNG (fallback), false si Textract a accepté le PDF direct
 */
public record OcrResult(
        boolean success,
        String text,
        int pageCount,
        ExtractionFailureReason failureMotif,
        boolean rasterized
) {
    public static OcrResult success(String text, int pageCount) {
        return new OcrResult(true, text, pageCount, null, false);
    }

    public static OcrResult successRasterized(String text, int pageCount) {
        return new OcrResult(true, text, pageCount, null, true);
    }

    public static OcrResult failure(ExtractionFailureReason motif) {
        return new OcrResult(false, null, 0, motif, false);
    }
}
