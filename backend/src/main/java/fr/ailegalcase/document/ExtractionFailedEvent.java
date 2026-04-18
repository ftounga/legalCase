package fr.ailegalcase.document;

import java.util.UUID;

/**
 * SF-121-02 : événement publié par {@link ExtractionService} quand une extraction passe en FAILED.
 * Déclenche la notification in-app et l'email au créateur du dossier.
 * Ne déclenche PAS le chunking ni l'analyse (contrairement à {@link ExtractionDoneEvent}).
 */
public record ExtractionFailedEvent(
        UUID extractionId,
        UUID documentId,
        String filename,
        ExtractionFailureReason reason
) {}
