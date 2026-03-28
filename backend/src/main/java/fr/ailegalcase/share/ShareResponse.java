package fr.ailegalcase.share;

import java.time.Instant;
import java.util.UUID;

public record ShareResponse(
        UUID id,
        String shareUrl,
        Instant expiresAt,
        Instant createdAt
) {}
