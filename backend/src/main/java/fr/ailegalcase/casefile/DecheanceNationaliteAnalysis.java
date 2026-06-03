package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-220-05 : entity 1:1 par dossier portant l'analyse de validité d'une mesure
 * de déchéance de nationalité (Code civil art. 25 et 25-1,
 * F-IM-51-decheance-nationalite-fr). Outil single-country FR.
 */
@Entity
@Table(name = "decheance_nationalite_analyses")
@Getter
@Setter
public class DecheanceNationaliteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "motif", length = 30)
    private String motif;

    @Column(name = "binational")
    private Boolean binational;

    @Column(name = "date_acquisition_nationalite")
    private LocalDate dateAcquisitionNationalite;

    @Column(name = "date_faits")
    private LocalDate dateFaits;

    @Column(name = "mesure_prononcee", nullable = false)
    private boolean mesurePrononcee;

    @Column(name = "date_decret")
    private LocalDate dateDecret;

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
