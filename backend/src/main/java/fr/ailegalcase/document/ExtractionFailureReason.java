package fr.ailegalcase.document;

/**
 * Motif de l'échec d'une extraction de document.
 * Associé à {@link ExtractionStatus#FAILED} pour qualifier l'erreur
 * et permettre au frontend d'afficher un message adapté à l'avocat.
 *
 * Introduit par SF-121-01. Étendu probablement par F-122 (OCR) avec
 * {@code OCR_QUOTA_EXCEEDED}.
 */
public enum ExtractionFailureReason {
    /** Parsing réussi mais texte vide après trim — cas typique du PDF scanné sans OCR. */
    EMPTY_TEXT,
    /** Content type non supporté (ex. .odt, .pages) — {@code IllegalArgumentException} dans parseText. */
    UNSUPPORTED_FORMAT,
    /** Erreur de parsing du fichier (PDF endommagé, DOCX corrompu). */
    CORRUPTED,
    /** Toute autre exception au cours de l'extraction (S3 download, IOException générique, etc.). */
    EXTRACTION_EXCEPTION
}
