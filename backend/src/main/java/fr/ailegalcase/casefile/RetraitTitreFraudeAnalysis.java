package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-41 : entity 1:1 par dossier portant l'analyse de validité d'un retrait
 * de titre de séjour pour fraude (art. L. 412-7 CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "retrait_titre_fraude_analyses")
@Getter
@Setter
public class RetraitTitreFraudeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_retrait", nullable = false)
    private LocalDate dateRetrait;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_retrait", nullable = false, length = 40)
    private RetraitTitreFraudeMotifEnum motifRetrait;

    @Column(name = "mise_en_demeure_prealable", nullable = false)
    private boolean miseEnDemeurePrealable;

    @Column(name = "date_mise_en_demeure")
    private LocalDate dateMiseEnDemeure;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private RetraitTitreFraudeStatut statut;

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
