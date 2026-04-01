package fr.ailegalcase.analysis;

import java.util.UUID;

public record ProcedureCheckResponse(
        UUID id,
        int ordre,
        String description,
        String statut
) {
    static ProcedureCheckResponse from(ProcedureCheck check) {
        return new ProcedureCheckResponse(
                check.getId(),
                check.getOrdre(),
                check.getDescription(),
                check.getStatut().name()
        );
    }
}
