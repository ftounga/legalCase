package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-47 : entity 1:1 par dossier portant l'analyse du congé de proche aidant
 * (art. L.3142-16 à L.3142-27 CT, F-DT-79). Outil single-country FR.
 */
@Entity
@Table(name = "conge_proche_aidant_analyses")
@Getter
@Setter
public class CongeProcheAidantAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private CongeProcheAidantStatut statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "lien_personne_aidee", nullable = false, length = 40)
    private CongeProcheAidantLien lienPersonneAidee;

    @Column(name = "personne_aidee_reside_france", nullable = false)
    private boolean personneAideeResideFrance;

    @Column(name = "duree_souhaitee_mois", nullable = false)
    private int dureeSouhaiteeMois;

    @Column(name = "duree_max_mois", nullable = false)
    private int dureeMaxMois;

    @Column(name = "duree_retenue_mois")
    private Integer dureeRetenueMois;

    @Column(name = "ajpa_demandee", nullable = false)
    private boolean ajpaDemandee;

    @Column(name = "estimation_ajpa")
    private BigDecimal estimationAjpa;

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
