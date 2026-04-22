package fr.ailegalcase.document;

import java.util.UUID;

/**
 * SF-145-01 : résumé readonly d'une pièce identifiée pour exposition UI.
 *
 * <p>SF-148-01 : ajout optionnel de {@link #visualDescription} produit par
 * Claude Vision quand la pièce a été enrichie. Null pour les pièces non
 * enrichies (mode OCR-only ou feature désactivée).
 */
public record DocumentPieceSummary(
        UUID id,
        String type,
        String label,
        int pageStart,
        int pageEnd,
        int orderIndex,
        String visualDescription
) {
    public static DocumentPieceSummary from(DocumentPiece p) {
        return new DocumentPieceSummary(
                p.getId(),
                p.getType().name(),
                p.getLabel(),
                p.getPageStart(),
                p.getPageEnd(),
                p.getOrderIndex(),
                p.getVisualDescription()
        );
    }
}
