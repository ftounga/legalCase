package fr.ailegalcase.audit;

import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String userEmail,
        UUID caseFileId,
        String caseFileTitle,
        String documentName,
        Instant createdAt
) {}
