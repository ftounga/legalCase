package fr.ailegalcase.casefile;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CaseDeadlineResponse(
        UUID id,
        String label,
        LocalDate dueDate,
        String source,
        String aiStatus,
        Instant createdAt,
        Instant updatedAt
) {
    static CaseDeadlineResponse from(CaseDeadline d) {
        return new CaseDeadlineResponse(d.getId(), d.getLabel(), d.getDueDate(),
                d.getSource(), d.getAiStatus(), d.getCreatedAt(), d.getUpdatedAt());
    }
}
