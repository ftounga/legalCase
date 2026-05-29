package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-214-05 : entity 1:1 par dossier portant l'analyse d'éligibilité à la VPF
 * liens personnels et familiaux L. 423-23 CESEDA (art. 8 CEDH). Outil
 * single-country FR.
 */
@Entity
@Table(name = "vpf_liens_personnels_analyses")
@Getter
@Setter
public class VpfLiensPersonnelsAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "duree_residence_france_mois", nullable = false)
    private int dureeResidenceFranceMois;

    @Column(name = "entree_en_france_mineur", nullable = false)
    private boolean entreeEnFranceMineur;

    @Column(name = "enfants_en_france", nullable = false)
    private boolean enfantsEnFrance;

    @Column(name = "conjoint_en_france", nullable = false)
    private boolean conjointEnFrance;

    @Column(name = "parents_en_france", nullable = false)
    private boolean parentsEnFrance;

    @Column(name = "situation_familiale_a_letranger", length = 300)
    private String situationFamilialeAlEtranger;

    @Column(name = "niveau_integration", nullable = false, length = 20)
    private String niveauIntegration;

    @Column(name = "ancienne_conviction_penale", nullable = false)
    private boolean ancienneConvictionPenale;

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
