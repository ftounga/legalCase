package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-14-02 : entity 1:1 par dossier portant l'analyse régularisation 9ter
 * médical BE (art. 9ter Loi 15/12/1980 + AR 17/05/2007 art. 7-8). Outil
 * single-country BELGIQUE.
 */
@Entity
@Table(name = "belgian_9ter_analyses")
@Getter
@Setter
public class Belgian9terAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_symptomes")
    private LocalDate dateDebutSymptomes;

    @Column(name = "maladie_grave_certifiee", nullable = false)
    private boolean maladieGraveCertifiee;

    @Column(name = "soins_necessaires_disponibles_be", nullable = false)
    private boolean soinsNecessairesDisponiblesBe;

    @Column(name = "soins_inaccessibles_pays_origine", nullable = false)
    private boolean soinsInaccessiblesPaysOrigine;

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
