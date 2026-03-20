package fr.ailegalcase.superadmin;

import java.util.UUID;

public record SuperAdminUserResponse(UUID id, String email, String firstName, String lastName, int workspaceCount) {
}
