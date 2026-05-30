package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-21 : entity 1:1 par dossier portant l'analyse du régime du stagiaire
 * (gratification minimale + requalification en CDI, art. L.124-1 et s. du code
 * de l'éducation, F-DT-109). Outil single-country FR.
 */
@Entity
@Table(name = "stagiaire_gratification_analyses")
@Getter
@Setter
public class StagiaireGratificationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_stage", nullable = false)
    private LocalDate dateDebutStage;

    @Column(name = "date_fin_stage", nullable = false)
    private LocalDate dateFinStage;

    @Column(name = "nombre_jours_presence", nullable = false)
    private int nombreJoursPresence;

    @Column(name = "seuil_atteint", nullable = false)
    private boolean seuilAtteint;

    @Column(name = "gratification_obligatoire", nullable = false)
    private boolean gratificationObligatoire;

    @Column(name = "gratification_minimale_due")
    private BigDecimal gratificationMinimaleDue;

    @Column(name = "rappel_gratification")
    private BigDecimal rappelGratification;

    @Column(name = "depassement_duree_max", nullable = false)
    private boolean depassementDureeMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_requalification", nullable = false, length = 20)
    private StagiaireRisqueRequalification risqueRequalification;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_global", nullable = false, length = 30)
    private StagiaireGratificationVerdict verdictGlobal;

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
