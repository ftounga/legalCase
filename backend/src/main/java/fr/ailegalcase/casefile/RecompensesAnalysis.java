package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-FA-15-01 : entity 1:1 par dossier portant l'analyse des récompenses
 * (art. 1437 et 1469 Cciv). Outil single-country FR DROIT_FAMILLE.
 */
@Entity
@Table(name = "recompenses_analyses")
@Getter
@Setter
public class RecompensesAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "regime_matrimonial", nullable = false, length = 40)
    private String regimeMatrimonial;

    /** JSON sérialisé de la liste des opérations soumises. */
    @Column(name = "operations_json", nullable = false, columnDefinition = "TEXT")
    private String operationsJson = "[]";

    @Column(name = "country", nullable = false, length = 20)
    private String country;

    /** JSON sérialisé du {@link RecompensesResult}. */
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
