package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-FA-24-03 : entity 1:1 par dossier portant l'analyse de validité d'un
 * testament (FR — art. 967-1035 + 901-911 Cciv).
 */
@Entity
@Table(name = "testament_validite_analyses")
@Getter
@Setter
public class TestamentValiditeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "forme_testament", nullable = false, length = 32)
    private TestamentValiditeCalculator.FormeTestament formeTestament;

    @Column(name = "date_redaction", nullable = false, length = 32)
    private String dateRedaction;

    @Column(name = "age_testateur_ans", nullable = false)
    private int ageTestateurAnsRedaction;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_validite", nullable = false, length = 16)
    private TestamentValiditeCalculator.VerdictValidite verdictValidite;

    @Column(name = "action_en_reduction_possible", nullable = false)
    private boolean actionEnReductionPossible;

    @Column(name = "score_eligibilite", nullable = false)
    private int scoreEligibilite;

    @Column(name = "country", nullable = false, length = 20)
    private String country;

    @Column(name = "result_data", nullable = false, columnDefinition = "TEXT")
    private String resultData = "{}";

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
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
