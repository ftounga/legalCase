package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-25-01 : entity 1:1 par dossier portant l'analyse d'indemnité compensatrice
 * de préavis FR (art. L.1234-1 Code du travail + CCN).
 */
@Entity
@Table(name = "indemnite_preavis_analyses")
@Getter
@Setter
public class IndemnitePreavisAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "anciennete_mois", nullable = false)
    private int ancienneteMois;

    @Column(name = "fonction", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private IndemnitePreavisFonction fonction;

    @Column(name = "convention_collective_code", length = 30)
    private String conventionCollectiveCode;

    @Column(name = "salaire_mensuel_brut_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuelBrutEur;

    @Column(name = "exemption_employeur", nullable = false)
    private boolean exemptionEmployeur;

    @Column(name = "date_rupture", nullable = false)
    private LocalDate dateRupture;

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
