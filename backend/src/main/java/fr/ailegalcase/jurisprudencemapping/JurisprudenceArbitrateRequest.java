package fr.ailegalcase.jurisprudencemapping;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * F-JU-01 / SF-JU-01-05 — décision d'arbitrage d'un flag PENDING.
 */
public record JurisprudenceArbitrateRequest(

        @NotNull
        JurisprudenceWatchFlagDecision decision,

        @Size(max = 2000)
        String comment) {
}
