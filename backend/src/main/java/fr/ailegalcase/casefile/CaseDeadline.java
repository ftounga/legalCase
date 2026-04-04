package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "case_deadlines")
@Getter
@Setter
public class CaseDeadline {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @Column(nullable = false, length = 255)
    private String label;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 10)
    private String source = "MANUAL";

    @Column(length = 20)
    private String aiStatus;

    @Column(length = 50)
    private String alertThresholds;

    @Column(length = 50)
    private String documentType;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
