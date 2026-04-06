package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TribunalTravailFicheResponse(
        UUID id,
        TribunalTravailFicheRequest.Requerant requerant,
        TribunalTravailFicheRequest.Defendeur defendeur,
        TribunalTravailFicheRequest.ProcedureInfo procedureInfo,
        TribunalTravailFicheRequest.ContratInfo contratInfo,
        List<TribunalTravailFicheRequest.Demande> demandes,
        String exposeDesMoyens,
        List<PieceEntry> piecesList,
        Instant updatedAt
) {
    public record PieceEntry(int numero, String nom) {}
}
