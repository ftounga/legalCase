package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-18-07 : entity 1:1 par dossier portant l'analyse de recevabilité
 * d'une possession d'état (FR — art. 311-1 + 311-2 + 317 Cciv).
 */
@Entity
@Table(name = "possession_etat_analyses")
@Getter
@Setter
public class PossessionEtatAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_possession", nullable = false)
    private LocalDate dateDebutPossession;

    @Column(name = "date_fin_possession", nullable = false)
    private LocalDate dateFinPossession;

    @Column(name = "tractatus", nullable = false)
    private boolean tractatus;

    @Column(name = "fama", nullable = false)
    private boolean fama;

    @Column(name = "nomen", nullable = false)
    private boolean nomen;

    @Column(name = "continue_condition", nullable = false)
    private boolean continueCondition;

    @Column(name = "paisible", nullable = false)
    private boolean paisible;

    @Column(name = "non_equivoque", nullable = false)
    private boolean nonEquivoque;

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
