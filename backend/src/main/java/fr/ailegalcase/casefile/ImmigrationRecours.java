package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "immigration_recours")
@Getter
@Setter
public class ImmigrationRecours {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(nullable = false, length = 50)
    private String recoursType;

    @Column(nullable = false)
    private LocalDate dateNotification;

    @Column(nullable = false)
    private LocalDate dateLimite;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requerantData = "{}";

    @Column(nullable = false, columnDefinition = "TEXT")
    private String decisionContesteeData = "{}";

    @Column(columnDefinition = "TEXT")
    private String exposeFaits;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String generatedDocument = "{}";

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
