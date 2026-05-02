package fr.ailegalcase.backlog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backlog_sync_runs")
@Getter
@Setter
public class BacklogSyncRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "features_count", nullable = false)
    private int featuresCount;

    @Column(name = "subfeatures_count", nullable = false)
    private int subfeaturesCount;

    @Column(name = "marketing_count", nullable = false)
    private int marketingCount;

    @Column(name = "orphans_marked", nullable = false)
    private int orphansMarked;

    @Enumerated(EnumType.STRING)
    @Column(name = "triggered_by", nullable = false, length = 32)
    private SyncTrigger triggeredBy;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
