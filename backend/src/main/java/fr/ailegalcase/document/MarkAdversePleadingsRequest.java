package fr.ailegalcase.document;

import jakarta.validation.constraints.NotNull;

/**
 * SF-261-01 : payload de marquage « écritures adverses » d'un document.
 * Le champ est obligatoire (true active le marquage, false le retire) — un
 * body sans {@code adversePleadings} est rejeté en 400.
 */
public record MarkAdversePleadingsRequest(
        @NotNull(message = "adversePleadings is required")
        Boolean adversePleadings
) {}
