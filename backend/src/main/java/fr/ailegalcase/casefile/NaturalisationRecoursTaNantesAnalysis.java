package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-31 : entity 1:1 par dossier portant l'analyse du délai de recours
 * devant le Tribunal administratif de Nantes contre un refus de naturalisation
 * par décret (CJA L. 213-1, délai 2 mois ; Cciv 21-15). Outil single-country FR.
 */
@Entity
@Table(name = "naturalisation_recours_ta_analyses")
@Getter
@Setter
public class NaturalisationRecoursTaNantesAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_refus_decret", nullable = false)
    private LocalDate dateRefusDecret;

    @Column(name = "recours_prerequis", nullable = false)
    private boolean recoursPrerequis;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private NaturalisationRecoursTaNantesStatut statut;

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
