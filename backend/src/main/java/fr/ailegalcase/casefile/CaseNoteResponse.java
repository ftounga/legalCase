package fr.ailegalcase.casefile;

import java.time.Instant;
import java.util.UUID;

public record CaseNoteResponse(
        UUID id,
        String content,
        String authorEmail,
        Instant createdAt,
        Instant updatedAt
) {
    static CaseNoteResponse from(CaseNote note) {
        return new CaseNoteResponse(
                note.getId(),
                note.getContent(),
                note.getCreatedBy().getEmail(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
