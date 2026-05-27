package fr.ailegalcase.jurisprudencemapping;

import java.time.Instant;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-10 — payload du GET /api/admin/jurisprudence/bootstrap/jobs/{id}.
 */
public record JurisprudenceBootstrapJobStatusResponse(
        UUID jobId,
        JurisprudenceBootstrapJobStatus status,
        int entriesTotal,
        int entriesProcessed,
        int mappingsCreated,
        int entriesSkipped,
        Long durationMs,
        String errorMessage,
        Instant startedAt,
        Instant completedAt) {

    public static JurisprudenceBootstrapJobStatusResponse from(JurisprudenceBootstrapJob job) {
        return new JurisprudenceBootstrapJobStatusResponse(
                job.getId(),
                job.getStatus(),
                job.getEntriesTotal(),
                job.getEntriesProcessed(),
                job.getMappingsCreated(),
                job.getEntriesSkipped(),
                job.getDurationMs(),
                job.getErrorMessage(),
                job.getStartedAt(),
                job.getCompletedAt());
    }
}
