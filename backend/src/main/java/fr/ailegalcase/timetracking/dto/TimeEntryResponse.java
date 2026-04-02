package fr.ailegalcase.timetracking.dto;

import fr.ailegalcase.timetracking.entity.TimeEntry;

import java.time.Instant;
import java.util.UUID;

public record TimeEntryResponse(
        UUID id,
        UUID caseFileId,
        UUID userId,
        Instant startedAt,
        Instant stoppedAt,
        Integer durationSeconds
) {
    public static TimeEntryResponse from(TimeEntry entry) {
        return new TimeEntryResponse(
                entry.getId(),
                entry.getCaseFile().getId(),
                entry.getUser().getId(),
                entry.getStartedAt(),
                entry.getStoppedAt(),
                entry.getDurationSeconds()
        );
    }
}
