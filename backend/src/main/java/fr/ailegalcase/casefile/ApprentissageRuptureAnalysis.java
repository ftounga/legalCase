package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-23 : entity 1:1 par dossier portant l'analyse de la validité de la
 * rupture du contrat d'apprentissage (art. L.6222-18 et s. CT, F-DT-110). Outil
 * single-country FR.
 */
@Entity
@Table(name = "apprentissage_rupture_analyses")
@Getter
@Setter
public class ApprentissageRuptureAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_contrat", nullable = false)
    private LocalDate dateDebutContrat;

    @Column(name = "date_rupture", nullable = false)
    private LocalDate dateRupture;

    @Enumerated(EnumType.STRING)
    @Column(name = "auteur_rupture", nullable = false, length = 20)
    private ApprentissageAuteurRupture auteurRupture;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_rupture", nullable = false, length = 30)
    private ApprentissageMotifRupture motifRupture;

    @Column(name = "jours_depuis_debut", nullable = false)
    private long joursDepuisDebut;

    @Enumerated(EnumType.STRING)
    @Column(name = "periode", nullable = false, length = 30)
    private ApprentissagePeriodeRupture periode;

    @Column(name = "dans_periode_libre", nullable = false)
    private boolean dansPeriodeLibre;

    @Enumerated(EnumType.STRING)
    @Column(name = "validite", nullable = false, length = 20)
    private ApprentissageRuptureValidite validite;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_global", nullable = false, length = 30)
    private ApprentissageRuptureVerdict verdictGlobal;

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
