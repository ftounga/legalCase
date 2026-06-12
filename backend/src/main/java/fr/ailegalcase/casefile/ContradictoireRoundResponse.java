package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-282 — un round du cycle contradictoire (DTO de réponse).
 */
public record ContradictoireRoundResponse(
        UUID id,
        int roundNumber,
        ContradictoireParty party,
        String label,
        LocalDate datedAt,
        LocalDate responseDueAt,
        UUID sourceDocumentId,
        UUID sourceConclusionId,
        Instant createdAt,
        Instant updatedAt
) {
    static ContradictoireRoundResponse from(ContradictoireRound r) {
        return new ContradictoireRoundResponse(
                r.getId(), r.getRoundNumber(), r.getParty(), r.getLabel(),
                r.getDatedAt(), r.getResponseDueAt(),
                r.getSourceDocumentId(), r.getSourceConclusionId(),
                r.getCreatedAt(), r.getUpdatedAt());
    }
}
