package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-DT-17-01 : entity 1:1 par dossier portant l'analyse d'indemnité de précarité
 * CDD (art. L.1243-8 Code du travail).
 */
@Entity
@Table(name = "cdd_indemnite_precarite_analyses")
@Getter
@Setter
public class IndemnitePrecariteCddAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "total_salaires_bruts", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalSalairesBruts;

    @Column(name = "taux_precarite", nullable = false)
    private int tauxPrecarite;

    @Column(name = "cas_exclusion", length = 50)
    private String casExclusion;

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
