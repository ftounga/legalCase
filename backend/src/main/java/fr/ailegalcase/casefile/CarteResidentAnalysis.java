package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-214-23 : entity 1:1 par dossier portant l'analyse d'éligibilité à la carte
 * de résident 10 ans L. 426-1 CESEDA. Outil single-country FR.
 */
@Entity
@Table(name = "carte_resident_analyses")
@Getter
@Setter
public class CarteResidentAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "duree_sejour_regulier_annees", nullable = false)
    private int dureeSejourRegulierAnnees;

    @Column(name = "types_titres_anterieurs")
    private String typesTitresAnterieurs;

    @Column(name = "niveau_integration", nullable = false, length = 20)
    private String niveauIntegration;

    @Column(name = "ressources_mensuelles_nettes", nullable = false)
    private double ressourcesMensuellesNettes;

    @Column(name = "condamnations_penales_graves", nullable = false)
    private boolean condamnationsPenalesGraves;

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
