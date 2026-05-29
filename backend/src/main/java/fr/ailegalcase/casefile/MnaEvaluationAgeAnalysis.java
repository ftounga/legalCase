package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-27 : entity 1:1 par dossier portant l'analyse de la procédure
 * d'évaluation d'âge d'un mineur non accompagné (MNA) refusé par l'ASE
 * (F-IM-38-mna-evaluation-age-fr, FRANCE uniquement).
 */
@Entity
@Table(name = "mna_evaluation_age_analyses")
@Getter
@Setter
public class MnaEvaluationAgeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_naissance_declaree", nullable = false)
    private LocalDate dateNaissanceDeclaree;

    @Column(name = "evaluation_ase_refusee", nullable = false)
    private boolean evaluationASERefusee;

    @Column(name = "date_refus_ase")
    private LocalDate dateRefusASE;

    @Column(name = "examen_osseux_ordonne", nullable = false)
    private boolean examenOsseuxOrdonne;

    @Column(name = "statut", nullable = false, length = 40)
    private String statut;

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
