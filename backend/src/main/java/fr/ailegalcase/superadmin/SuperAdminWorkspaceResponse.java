package fr.ailegalcase.superadmin;

import java.time.Instant;
import java.util.UUID;

public record SuperAdminWorkspaceResponse(
        UUID id,
        String name,
        String slug,
        String planCode,
        String status,
        Instant expiresAt,
        long memberCount,
        Instant createdAt
) {}
