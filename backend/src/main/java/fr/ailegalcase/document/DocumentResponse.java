package fr.ailegalcase.document;

import java.time.Instant;
import java.util.List;
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
        String failureReason,
        /**
         * SF-144-01 : true pendant l'appel synchrone à Textract. Lu par le polling
         * frontend pour afficher "Document scanné — OCR en cours".
         */
        boolean ocrRunning,
        /**
         * SF-144-01 : true si l'extraction finale a été produite via Textract
         * (c.-à-d. extraction_metadata contient "extractor":"textract" ou
         * "extractor":"textract-rasterized"). Utilisé pour afficher le chip `OCR`
         * dans la liste des docs.
         */
        boolean ocrExtracted,
        /**
         * SF-145-01 : pièces juridiques identifiées dans le document composite
         * (ex: un PDF contenant contrat + CNI + SMS = 3 entrées). Toujours au
         * moins 1 entrée en fallback `AUTRE` après extraction DONE. Vide si
         * l'extraction n'est pas encore DONE ou si la détection n'a pas tourné
         * (documents antérieurs à F-145).
         */
        List<DocumentPieceSummary> pieces
) {
    /** Constructeur rétrocompat 6 champs (avant SF-121-01). */
    public DocumentResponse(UUID id, UUID caseFileId, String originalFilename,
                             String contentType, long fileSize, Instant createdAt) {
        this(id, caseFileId, originalFilename, contentType, fileSize, createdAt, null, null, false, false, List.of());
    }

    /** Constructeur rétrocompat 8 champs (SF-121-01, avant SF-144-01). */
    public DocumentResponse(UUID id, UUID caseFileId, String originalFilename,
                             String contentType, long fileSize, Instant createdAt,
                             String extractionStatus, String failureReason) {
        this(id, caseFileId, originalFilename, contentType, fileSize, createdAt,
                extractionStatus, failureReason, false, false, List.of());
    }

    /** Constructeur rétrocompat 10 champs (SF-144-01, avant SF-145-01). */
    public DocumentResponse(UUID id, UUID caseFileId, String originalFilename,
                             String contentType, long fileSize, Instant createdAt,
                             String extractionStatus, String failureReason,
                             boolean ocrRunning, boolean ocrExtracted) {
        this(id, caseFileId, originalFilename, contentType, fileSize, createdAt,
                extractionStatus, failureReason, ocrRunning, ocrExtracted, List.of());
    }
}
