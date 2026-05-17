package fr.ailegalcase.consent.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * F-240 SF-240-01 : réponse 201 de {@code POST /api/v1/consent/accept} —
 * une entrée {@link Acceptance} par {@code consentType} du corps de requête.
 */
public record ConsentAcceptanceResponse(
        List<Acceptance> acceptances
) {

    /**
     * Une acceptation contractuelle persistée.
     *
     * @param workspaceId workspace primary de l'utilisateur, ou {@code null}
     *                    si l'acceptation précède la création du workspace
     */
    public record Acceptance(
            UUID id,
            UUID userId,
            String consentType,
            String version,
            Instant acceptedAt,
            String acceptanceIp,
            String acceptanceUserAgent,
            UUID workspaceId
    ) {
    }
}
