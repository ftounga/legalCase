package fr.ailegalcase.document;

import java.util.UUID;

/**
 * SF-145-01 : résumé readonly d'une pièce identifiée pour exposition UI.
 */
public record DocumentPieceSummary(
        UUID id,
        String type,
        String label,
        int pageStart,
        int pageEnd,
        int orderIndex
) {
    public static DocumentPieceSummary from(DocumentPiece p) {
        return new DocumentPieceSummary(
                p.getId(),
                p.getType().name(),
                p.getLabel(),
                p.getPageStart(),
                p.getPageEnd(),
                p.getOrderIndex()
        );
    }
}
