package fr.ailegalcase.workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId,
        String email,
        String firstName,
        String lastName,
        String memberRole,
        Instant createdAt
) {}
