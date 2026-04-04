package fr.ailegalcase.casefile;

import java.util.List;
import java.util.UUID;

public record ImmigrationChecklistResponse(
        UUID caseFileId,
        String titreType,
        String country,
        List<PieceItem> pieces
) {
    public record PieceItem(String label, String statut) {}
}
