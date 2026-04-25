package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-19-03 : entity 1:1 par dossier portant l'analyse d'acceptabilité
 * d'un changement de résidence enfant suite au déménagement d'un parent
 * (art. 373-2 al. 3 Cciv). Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "changement_residence_analyses")
@Getter
@Setter
public class ChangementResidenceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_changement_prevu", nullable = false)
    private LocalDate dateChangementPrevu;

    @Column(name = "distance_km", nullable = false)
    private int distanceKm;

    @Column(name = "raison_changement", length = 40, nullable = false)
    private String raisonChangement;

    @Column(name = "consentement_autre_parent", nullable = false)
    private boolean consentementAutreParent;

    @Column(name = "informe_prealablement", nullable = false)
    private boolean informePrealablement;

    @Column(name = "delai_information_jours", nullable = false)
    private int delaiInformationJours;

    @Column(name = "mode_residence_actuel", length = 40, nullable = false)
    private String modeResidenceActuel;

    @Column(name = "scolarite_impactee", nullable = false)
    private boolean scolariteImpactee;

    @Column(name = "modification_dvh_demandee", nullable = false)
    private boolean modificationDvhDemandee;

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
