package fr.ailegalcase.backlog;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class BacklogDtos {

    private BacklogDtos() {}

    public record BacklogFeatureSummary(
            UUID id,
            String code,
            String title,
            String targetVersion,
            BacklogStatus status,
            BacklogDomain domain,
            BacklogPriority priority,
            Instant updatedAt
    ) {}

    public record BacklogFeatureDetail(
            UUID id,
            String code,
            String title,
            String targetVersion,
            BacklogStatus status,
            String description,
            BacklogDomain domain,
            BacklogPriority priority,
            String sourceFile,
            Integer sourceLine,
            Instant parsedAt,
            Instant updatedAt,
            List<BacklogSubfeatureDto> subfeatures
    ) {}

    public record BacklogSubfeatureDto(
            UUID id,
            String code,
            String title,
            BacklogStatus status,
            String description,
            Integer sourceLine,
            Instant updatedAt
    ) {}

    public record BacklogMarketingTaskSummary(
            UUID id,
            String code,
            String title,
            BacklogMarketingStatus status,
            String category,
            Instant updatedAt
    ) {}

    public record BacklogSyncRunSummary(
            UUID id,
            Instant startedAt,
            Instant finishedAt,
            Long durationMs,
            boolean success,
            int featuresCount,
            int subfeaturesCount,
            int marketingCount,
            int orphansMarked,
            SyncTrigger triggeredBy,
            String errorMessage
    ) {}

    public record BacklogFreshness(
            Instant lastSyncAt,
            Instant lastSuccessAt,
            String status,
            Long minutesSinceLastSync
    ) {}

    public record BacklogSyncResult(
            UUID runId,
            long durationMs,
            int featuresCount,
            int subfeaturesCount,
            int marketingCount,
            int orphansMarked,
            boolean success
    ) {}
}
