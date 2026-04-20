package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-132-01 : entity 1:1 par dossier portant l'analyse d'indemnité de rupture
 * conventionnelle (art. R1234-2). Séparé de {@link IndemniteComparatif} qui ne
 * gérera plus que le barème Macron après SF-132-02.
 */
@Entity
@Table(name = "rupture_conv_indemnite_analyses")
@Getter
@Setter
public class RuptureConvIndemniteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "anciennete_annees", nullable = false)
    private int ancienneteAnnees;

    @Column(name = "salaire_mensuel", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuel;

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
