package fr.ailegalcase.analysis;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_diff_cache")
@Getter
@Setter
public class AnalysisDiffCache {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "from_id", nullable = false)
    private UUID fromId;

    @Column(name = "to_id", nullable = false)
    private UUID toId;

    @Column(name = "result_json", nullable = false, columnDefinition = "text")
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
