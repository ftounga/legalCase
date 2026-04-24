package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-14-03 : entity 1:1 par dossier portant l'analyse du regroupement familial
 * d'un citoyen UE (non-Belge) en Belgique — art. 40bis Loi 15/12/1980.
 * Outil single-country BE.
 */
@Entity
@Table(name = "belgian_40bis_analyses")
@Getter
@Setter
public class Belgian40bisAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "lien_familial", nullable = false, length = 40)
    private String lienFamilial;

    @Column(name = "regroupant_citoyen_ue", nullable = false)
    private boolean regroupantCitoyenUe;

    @Column(name = "regroupant_activite_categorie", nullable = false, length = 40)
    private String regroupantActiviteCategorie;

    @Column(name = "ressources_suffisantes", nullable = false)
    private boolean ressourcesSuffisantes;

    @Column(name = "assurance_maladie_ue", nullable = false)
    private boolean assuranceMaladieUe;

    @Column(name = "logement_suffisant", nullable = false)
    private boolean logementSuffisant;

    @Column(name = "menace_ordre_public", nullable = false)
    private boolean menaceOrdrePublic;

    @Column(name = "date_depot_demande")
    private LocalDate dateDepotDemande;

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
