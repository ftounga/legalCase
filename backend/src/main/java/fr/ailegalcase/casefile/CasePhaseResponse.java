package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * F-283 / SF-283-01 — projection d'une transition de phase pour le client.
 */
public record CasePhaseResponse(
        UUID id,
        CasePhaseType phase,
        String label,
        LocalDate enteredAt,
        String note,
        Instant createdAt,
        Instant updatedAt
) {
    public static CasePhaseResponse from(CasePhase p) {
        return new CasePhaseResponse(
                p.getId(),
                p.getPhase(),
                p.getLabel(),
                p.getEnteredAt(),
                p.getNote(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }
}
