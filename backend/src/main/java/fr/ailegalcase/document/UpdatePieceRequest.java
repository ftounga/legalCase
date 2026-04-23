package fr.ailegalcase.document;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * SF-145-11 : payload de mise à jour d'une pièce identifiée (type + label).
 * Le label peut être vide/null pour supprimer le label humain.
 */
public record UpdatePieceRequest(
        @NotBlank(message = "type is required")
        String type,
        @Size(max = 500, message = "label must be at most 500 characters")
        String label
) {}
