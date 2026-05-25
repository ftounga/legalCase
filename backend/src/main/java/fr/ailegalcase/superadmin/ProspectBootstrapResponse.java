package fr.ailegalcase.superadmin;

import java.time.Instant;
import java.util.UUID;

/**
 * F-251 SF-251-03 — réponse {@code 201 Created} de l'endpoint super-admin
 * {@code POST /api/v1/super-admin/prospect-bootstrap}.
 *
 * <p>{@code expiresAt} est la valeur calculée et persistée pour la nouvelle
 * souscription FREE — non null par construction (assurée par le calcul
 * applicatif + filet de sécurité {@code @PrePersist} SF-251-02).
 */
public record ProspectBootstrapResponse(
        UUID userId,
        UUID workspaceId,
        String workspaceName,
        Instant expiresAt
) {}
