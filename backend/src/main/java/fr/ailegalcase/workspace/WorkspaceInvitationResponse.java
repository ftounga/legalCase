package fr.ailegalcase.workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceInvitationResponse(
        UUID id,
        String email,
        String role,
        String status,
        Instant expiresAt,
        Instant createdAt
) {}
