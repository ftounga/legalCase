package fr.ailegalcase.document;

import java.util.UUID;

/**
 * SF-145-01 : résumé readonly d'une pièce identifiée pour exposition UI.
 *
 * <p>SF-148-01 : ajout optionnel de {@link #visualDescription} produit par
 * LegalCase Vision quand la pièce a été enrichie. Null pour les pièces non
 * enrichies (mode OCR-only ou feature désactivée).
 *
 * <p>SF-148-03 : ajout de {@link #visionStatus} pour que le frontend sache
 * afficher un spinner si enrichissement en cours.
 */
public record DocumentPieceSummary(
        UUID id,
        String type,
        String label,
        int pageStart,
        int pageEnd,
        int orderIndex,
        String visualDescription,
        String visionStatus
) {
    public static DocumentPieceSummary from(DocumentPiece p) {
        return new DocumentPieceSummary(
                p.getId(),
                p.getType().name(),
                p.getLabel(),
                p.getPageStart(),
                p.getPageEnd(),
                p.getOrderIndex(),
                p.getVisualDescription(),
                p.getVisionStatus() != null ? p.getVisionStatus().name() : VisionStatus.NOT_APPLICABLE.name()
        );
    }
}
