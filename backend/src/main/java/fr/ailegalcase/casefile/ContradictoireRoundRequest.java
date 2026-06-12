package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * F-282 — création / mise à jour d'un round du cycle contradictoire.
 * La cohérence {@code responseDueAt >= datedAt} est validée dans le service (400 sinon).
 */
public record ContradictoireRoundRequest(
        @NotNull(message = "party is required")
        ContradictoireParty party,

        @Size(max = 200, message = "label must not exceed 200 characters")
        String label,

        @NotNull(message = "datedAt is required")
        LocalDate datedAt,

        LocalDate responseDueAt,

        UUID sourceDocumentId,

        UUID sourceConclusionId
) {}
