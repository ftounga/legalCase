package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-03 : entity 1:1 par dossier portant l'analyse de l'exécution forcée
 * d'un jugement CPH (art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 et s. Code
 * travail). Outil single-country FR.
 */
@Entity
@Table(name = "execution_jugement_cph_analyses")
@Getter
@Setter
public class ExecutionJugementCphAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_jugement", nullable = false)
    private LocalDate dateJugement;

    @Column(name = "montant_condamnation", nullable = false)
    private double montantCondamnation;

    @Column(name = "execution_provisoire_ordonnee", nullable = false)
    private boolean executionProvisoireOrdonnee;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation_employeur", nullable = false, length = 20)
    private ExecutionJugementCphSituationEmployeur situationEmployeur;

    @Column(name = "date_ouverture_procedure_collective")
    private LocalDate dateOuvertureProcedureCollective;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private ExecutionJugementCphVerdict verdict;

    @Column(name = "ags_eligible", nullable = false)
    private boolean agsEligible;

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
