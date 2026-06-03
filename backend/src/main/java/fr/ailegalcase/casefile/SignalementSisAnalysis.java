package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-220-06 : entity 1:1 par dossier portant l'analyse de contestation /
 * radiation d'un signalement SIS aux fins de non-admission (Règl. UE 2018/1860 /
 * CESEDA L.312-3, F-IM-52-signalement-sis-fr). Outil single-country FR.
 */
@Entity
@Table(name = "signalement_sis_analyses")
@Getter
@Setter
public class SignalementSisAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "signalement_connu")
    private Boolean signalementConnu;

    @Column(name = "etat_signalant", length = 30)
    private String etatSignalant;

    @Column(name = "motif_signalement", length = 40)
    private String motifSignalement;

    @Column(name = "titre_sejour_valide")
    private Boolean titreSejourValide;

    @Column(name = "date_signalement")
    private LocalDate dateSignalement;

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
