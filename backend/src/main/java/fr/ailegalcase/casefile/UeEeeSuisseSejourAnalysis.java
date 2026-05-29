package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-214-39 : entity 1:1 par dossier portant l'analyse du droit au séjour
 * UE/EEE/Suisse en France (directive 2004/38, L. 233-1+ CESEDA). Outil
 * single-country FR.
 */
@Entity
@Table(name = "ue_eee_suisse_sejour_analyses")
@Getter
@Setter
public class UeEeeSuisseSejourAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "nationalite")
    private String nationalite;

    @Column(name = "est_citoyen_ue", nullable = false)
    private boolean estCitoyenUE;

    @Column(name = "membre_famille_non_ue", nullable = false)
    private boolean membreFamilleNonUE;

    @Column(name = "duree_sejour_mois", nullable = false)
    private int dureeSejourMois;

    @Column(name = "activite_professionnelle", nullable = false, length = 50)
    private String activiteProfessionnelle;

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
