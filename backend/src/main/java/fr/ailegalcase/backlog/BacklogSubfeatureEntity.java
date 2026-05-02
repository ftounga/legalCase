package fr.ailegalcase.backlog;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "backlog_subfeatures")
@Getter
@Setter
public class BacklogSubfeatureEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64, unique = true)
    private String code;

    @Column(name = "parent_feature_id", nullable = false)
    private UUID parentFeatureId;

    @Column(length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BacklogStatus status;

    @Column(columnDefinition = "TEXT")
    private String description;

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
