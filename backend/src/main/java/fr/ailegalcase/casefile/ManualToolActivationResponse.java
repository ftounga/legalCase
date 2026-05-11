package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-238-03 : réponse de l'endpoint POST activation manuelle.
 */
public record ManualToolActivationResponse(
        UUID id,
        String toolId,
        Instant activatedAt
) {
}
