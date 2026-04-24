package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-09-04 : entity 1:1 par dossier portant l'analyse AES voie étudiante
 * (circulaire Valls 28/11/2012 actualisée + L.412-1 CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "aes_etudiant_analyses")
@Getter
@Setter
public class AesEtudiantAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_entree_france", nullable = false)
    private LocalDate dateEntreeFrance;

    @Column(name = "duree_presence_mois", nullable = false)
    private int dureePresenceMois;

    @Column(name = "annees_scolarite_en_france_consecutives", nullable = false)
    private int anneesScolariteEnFranceConsecutives;

    @Column(name = "niveau_etudes_actuel", nullable = false, length = 30)
    private String niveauEtudesActuel;

    @Column(name = "resultats_academiques", nullable = false, length = 30)
    private String resultatsAcademiques;

    @Column(name = "inscription_etablissement_reconnu", nullable = false)
    private boolean inscriptionEtablissementReconnu;

    @Column(name = "moyens_subsistance", nullable = false)
    private boolean moyensSubsistance;

    @Column(name = "menace_ordre_public", nullable = false)
    private boolean menaceOrdrePublic;

    @Column(name = "parcours_coherent", nullable = false)
    private boolean parcoursCoherent;

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
