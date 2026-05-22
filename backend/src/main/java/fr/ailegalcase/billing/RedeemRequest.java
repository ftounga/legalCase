package fr.ailegalcase.billing;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * F-255 SF-255-01 — body de redemption d'un code par un utilisateur d'un
 * workspace. Le {@code workspaceId} n'est pas dans le body (lu du path
 * obligatoirement, puis revérifié contre les memberships de l'utilisateur
 * authentifié — invariant d'isolation workspace).
 */
public record RedeemRequest(
        @NotBlank(message = "code is required")
        @Size(max = 64, message = "code must be at most 64 characters")
        String code
) {}
