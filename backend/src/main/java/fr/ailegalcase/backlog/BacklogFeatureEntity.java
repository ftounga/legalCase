package fr.ailegalcase.backlog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backlog_features")
@Getter
@Setter
public class BacklogFeatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 32, unique = true)
    private String code;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "target_version", length = 32)
    private String targetVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BacklogStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private BacklogDomain domain;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private BacklogPriority priority;

    @Column(name = "source_file", nullable = false, length = 255)
    private String sourceFile;

    @Column(name = "source_line")
    private Integer sourceLine;

    @Column(name = "parsed_at", nullable = false)
    private Instant parsedAt;

    @Column(name = "is_orphaned", nullable = false)
    private boolean orphaned;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }
}
