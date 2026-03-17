package fr.ailegalcase.workspace;

import java.util.UUID;

public record WorkspaceResponse(UUID id, String name, String slug, String planCode, String status) {
}
