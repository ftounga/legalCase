package fr.ailegalcase.ocr;

/**
 * SF-122-05 : preview du bouton "Relancer avec OCR" sur un dossier.
 *
 * @param failedDocsCount  nb de docs FAILED éligibles (motifs EMPTY_TEXT ou OCR_FAILED)
 * @param estimatedPages   somme des pages PDF (PDFBox) des docs éligibles
 * @param monthlyRemaining pages du quota mensuel restantes (plan uniquement, hors packs)
 * @param packsRemaining   pages restantes des packs achetés
 * @param canRetry         true si le retry est possible (quota suffisant)
 */
public record OcrRetryPreviewResponse(
        int failedDocsCount,
        int estimatedPages,
        int monthlyRemaining,
        int packsRemaining,
        boolean canRetry
) {}
