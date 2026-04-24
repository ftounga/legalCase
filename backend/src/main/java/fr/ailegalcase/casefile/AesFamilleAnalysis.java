package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-09-02 : entity 1:1 par dossier portant l'analyse AES voie familiale
 * (art. L.435-1 CESEDA + circulaire Valls 28/11/2012). Outil single-country FR.
 */
@Entity
@Table(name = "aes_famille_analyses")
@Getter
@Setter
public class AesFamilleAnalysis {

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

    @Column(name = "conjoint_francais_ou_regulier", nullable = false)
    private boolean conjointFrancaisOuRegulier;

    @Column(name = "enfants_scolarises_france", nullable = false)
    private int enfantsScolarisesFrance;

    @Column(name = "duree_scolarite_plus_ancien_enfant_annees", nullable = false)
    private int dureeScolaritePlusAncienEnfantAnnees;

    @Column(name = "preuves_insertion", nullable = false)
    private boolean preuvesInsertion;

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
