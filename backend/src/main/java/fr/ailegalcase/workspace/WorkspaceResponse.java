package fr.ailegalcase.workspace;

import java.time.Instant;
import java.util.UUID;

public record WorkspaceResponse(UUID id, String name, String slug, String planCode, String status, Instant expiresAt, boolean primary, String legalDomain, String country) {
}
