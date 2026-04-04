package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrudhomeFicheResponse(
        UUID id,
        PrudhomeFicheRequest.Demandeur demandeur,
        PrudhomeFicheRequest.Defendeur defendeur,
        List<PrudhomeFicheRequest.Demande> demandes,
        String faitsTexte,
        String moyensDroitTexte,
        List<PieceEntry> piecesList,
        Instant updatedAt
) {
    public record PieceEntry(int numero, String nom) {}
}
