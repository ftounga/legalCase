package fr.ailegalcase.auth;

import java.util.UUID;

public record MeResponse(UUID id, String email, String firstName, String lastName, String provider, boolean isSuperAdmin) {
}
