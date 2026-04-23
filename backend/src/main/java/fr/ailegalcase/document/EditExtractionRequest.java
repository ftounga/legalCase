package fr.ailegalcase.document;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * SF-149-01 : payload d'édition manuelle de l'extrait OCR.
 * Limite à 500 000 caractères (2,5× la limite d'affichage TEXT_TRUNCATE_LIMIT)
 * pour laisser une marge en cas de document très long.
 */
public record EditExtractionRequest(
        @NotNull(message = "extractedText is required")
        @Size(max = 500_000, message = "extractedText must be at most 500 000 characters")
        String extractedText
) {}
