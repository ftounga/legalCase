package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-17 : entity 1:1 par dossier portant l'analyse d'ouverture des droits à
 * l'ARE de l'intermittent du spectacle (annexes 8 et 10 Unedic — seuil 507 h /
 * 12 mois, F-DT-106). Outil single-country FR.
 */
@Entity
@Table(name = "intermittent_spectacle_are_analyses")
@Getter
@Setter
public class IntermittentSpectacleAreAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "annexe", nullable = false, length = 30)
    private IntermittentSpectacleAnnexe annexe;

    @Column(name = "date_fin_contrat", nullable = false)
    private LocalDate dateFinContrat;

    @Column(name = "heures_travaillees_12_mois", nullable = false)
    private int heuresTravaillees12Mois;

    @Column(name = "heures_cachets", nullable = false)
    private int heuresCachets;

    @Column(name = "heures_formation_retenues", nullable = false)
    private int heuresFormationRetenues;

    @Column(name = "heures_totales_retenues", nullable = false)
    private int heuresTotalesRetenues;

    @Column(name = "heures_manquantes", nullable = false)
    private int heuresManquantes;

    @Column(name = "heures_excedentaires", nullable = false)
    private int heuresExcedentaires;

    @Column(name = "date_prochain_examen", nullable = false)
    private LocalDate dateProchainExamen;

    @Enumerated(EnumType.STRING)
    @Column(name = "ouverture_droits", nullable = false, length = 20)
    private IntermittentSpectacleAreVerdict ouvertureDroits;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private IntermittentSpectacleAreStatut statut;

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
