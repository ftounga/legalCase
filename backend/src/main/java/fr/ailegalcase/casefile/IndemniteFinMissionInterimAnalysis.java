package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-18-01 : entity 1:1 par dossier portant l'analyse d'indemnité de fin
 * de mission intérim (art. L.1251-32 Code du travail).
 */
@Entity
@Table(name = "indemnite_fin_mission_interim_analyses")
@Getter
@Setter
public class IndemniteFinMissionInterimAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "total_remunerations_brutes_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRemunerationsBrutesEur;

    @Column(name = "duree_mission_jours", nullable = false)
    private int dureeMissionJours;

    @Column(name = "motif_exclusion", length = 60)
    private String motifExclusion;

    @Column(name = "date_fin_mission")
    private LocalDate dateFinMission;

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
