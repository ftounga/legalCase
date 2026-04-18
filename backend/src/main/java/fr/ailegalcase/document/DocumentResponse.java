package fr.ailegalcase.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID caseFileId,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt,
        /**
         * SF-121-01 : statut de l'extraction associée (PENDING / PROCESSING / DONE / FAILED).
         * null si aucune extraction n'a encore été créée pour ce document.
         */
        String extractionStatus,
        /**
         * SF-121-01 : motif d'échec si extractionStatus = FAILED.
         * Valeurs : EMPTY_TEXT / UNSUPPORTED_FORMAT / CORRUPTED / EXTRACTION_EXCEPTION.
         * null pour les extractions PENDING / PROCESSING / DONE.
         */
        String failureReason
) {
    /** Constructeur rétrocompat 6 champs (avant SF-121-01). */
    public DocumentResponse(UUID id, UUID caseFileId, String originalFilename,
                             String contentType, long fileSize, Instant createdAt) {
        this(id, caseFileId, originalFilename, contentType, fileSize, createdAt, null, null);
    }
}
