package fr.ailegalcase.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID caseFileId,
        String originalFilename,
        String contentType,
        long fileSize,
        Instant createdAt
) {}
