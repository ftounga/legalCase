package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-45 : entity 1:1 par dossier portant l'analyse du congé parental
 * d'éducation (art. L.1225-47 à L.1225-60 CT, F-DT-78). Outil single-country FR.
 */
@Entity
@Table(name = "conge_parental_education_analyses")
@Getter
@Setter
public class CongeParentalEducationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private CongeParentalEducationStatut statut;

    @Column(name = "anciennete_mois", nullable = false)
    private int ancienneteMois;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalite", nullable = false, length = 20)
    private CongeParentalEducationModalite modalite;

    @Column(name = "nombre_enfants", nullable = false)
    private int nombreEnfants;

    @Column(name = "date_naissance_ou_adoption", nullable = false)
    private LocalDate dateNaissanceOuAdoption;

    @Column(name = "date_fin_max")
    private LocalDate dateFinMax;

    @Column(name = "duree_max_mois", nullable = false)
    private int dureeMaxMois;

    @Column(name = "protection_reintegration", nullable = false)
    private boolean protectionReintegration;

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
