package fr.ailegalcase.jurisprudencemapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-05 — représentation API d'un {@link JurisprudenceAuditLog}.
 */
public record JurisprudenceAuditLogResponse(
        UUID id,
        UUID mappingId,
        JurisprudenceAuditAction action,
        JurisprudenceAuditActor actor,
        UUID actorUserId,
        BigDecimal claudeConfidence,
        String claudeReason,
        Instant createdAt) {

    public static JurisprudenceAuditLogResponse from(JurisprudenceAuditLog entry) {
        return new JurisprudenceAuditLogResponse(
                entry.getId(),
                entry.getMapping() != null ? entry.getMapping().getId() : null,
                entry.getAction(),
                entry.getActor(),
                entry.getActorUser() != null ? entry.getActorUser().getId() : null,
                entry.getClaudeConfidence(),
                entry.getClaudeReason(),
                entry.getCreatedAt());
    }
}
