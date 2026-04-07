package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "immigration_title_decisions")
@Getter
@Setter
public class ImmigrationTitleDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(nullable = false, length = 20)
    private String country;

    @Column(nullable = false)
    private boolean nationaliteUe;

    @Column(nullable = false, length = 30)
    private String motif;

    @Column(nullable = false, length = 20)
    private String duree;

    @Column(length = 30)
    private String situationFamiliale;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendedTitles = "[]";

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
