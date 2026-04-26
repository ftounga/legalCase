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
 * SF-FA-18-09 : entity 1:1 par dossier portant l'analyse de recevabilité
 * d'une adoption (FR — art. 343-370-2 Cciv).
 */
@Entity
@Table(name = "adoption_analyses")
@Getter
@Setter
public class AdoptionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "forme_adoption", nullable = false, length = 20)
    private AdoptionCalculator.FormeAdoption formeAdoption;

    @Column(name = "age_adoptant", nullable = false)
    private int ageAdoptant;

    @Column(name = "age_adopte", nullable = false)
    private int ageAdopte;

    @Column(name = "consentement_parents", nullable = false)
    private boolean consentementParents;

    @Column(name = "consentement_adopte", nullable = false)
    private boolean consentementAdopte;

    @Column(name = "consentement_conjoint_adoptant", nullable = false)
    private boolean consentementConjointAdoptant;

    @Column(name = "enquetes", nullable = false)
    private boolean enquetes;

    @Column(name = "placement6mois", nullable = false)
    private boolean placement6mois;

    @Column(name = "pupille_etat", nullable = false)
    private boolean pupilleEtat;

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
