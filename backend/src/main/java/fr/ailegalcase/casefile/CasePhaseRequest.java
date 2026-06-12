package fr.ailegalcase.casefile;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * F-283 / SF-283-01 — création / mise à jour d'une transition de phase
 * procédurale. {@code phase} appartient au référentiel {@link CasePhaseType}
 * (désérialisation stricte : une valeur hors enum → 400).
 */
public record CasePhaseRequest(
        @NotNull(message = "phase is required")
        CasePhaseType phase,

        @Size(max = 200, message = "label must not exceed 200 characters")
        String label,

        @NotNull(message = "enteredAt is required")
        LocalDate enteredAt,

        @Size(max = 2000, message = "note must not exceed 2000 characters")
        String note
) {}
