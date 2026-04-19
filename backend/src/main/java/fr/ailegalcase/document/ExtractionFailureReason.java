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
    /** Parsing réussi mais texte vide après trim — PDF scanné sans OCR + OCR désactivé/ sans résultat. */
    EMPTY_TEXT,
    /** Content type non supporté (ex. .odt, .pages) — {@code IllegalArgumentException} dans parseText. */
    UNSUPPORTED_FORMAT,
    /** Erreur de parsing du fichier (PDF endommagé, DOCX corrompu). */
    CORRUPTED,
    /** Toute autre exception au cours de l'extraction (S3 download, IOException générique, etc.). */
    EXTRACTION_EXCEPTION,
    /** SF-122-01 : OCR Textract a levé une exception ou renvoyé une erreur. */
    OCR_FAILED,
    /** SF-122-01 : document trop volumineux pour l'API Textract sync (>5 Mo ou >11 pages). */
    OCR_UNSUPPORTED_SIZE,
    /** SF-122-02 : quota OCR mensuel ou hard cap journalier dépassé. */
    OCR_QUOTA_EXCEEDED
}
