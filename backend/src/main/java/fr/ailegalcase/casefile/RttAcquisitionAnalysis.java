package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-49 : entity 1:1 par dossier portant l'analyse de l'acquisition de JRTT
 * selon un accord d'aménagement du temps de travail (art. L.3121-41 à L.3121-44
 * CT, F-DT-80). Outil single-country FR.
 */
@Entity
@Table(name = "rtt_acquisition_analyses")
@Getter
@Setter
public class RttAcquisitionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private RttAcquisitionStatut statut;

    @Column(name = "horaire_hebdomadaire_collectif", nullable = false)
    private double horaireHebdomadaireCollectif;

    @Column(name = "accord_collectif_present", nullable = false)
    private boolean accordCollectifPresent;

    @Column(name = "semaines_travaillees_an", nullable = false)
    private int semainesTravailleesAn;

    @Column(name = "nombre_jrtt_theorique")
    private Double nombreJrttTheorique;

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
