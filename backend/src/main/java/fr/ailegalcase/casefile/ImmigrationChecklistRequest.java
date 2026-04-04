package fr.ailegalcase.casefile;

import java.util.List;

public record ImmigrationChecklistRequest(
        String titreType,
        String country,
        List<PieceItemRequest> pieces
) {
    public record PieceItemRequest(String label, String statut) {}
}
