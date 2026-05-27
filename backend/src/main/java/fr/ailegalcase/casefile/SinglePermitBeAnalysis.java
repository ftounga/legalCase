package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-01 : entity 1:1 par dossier portant l'analyse d'un permit unique belge
 * (Loi du 30/04/1999 + AR du 02/09/2018). Outil single-country BELGIQUE.
 */
@Entity
@Table(name = "single_permit_be_analyses")
@Getter
@Setter
public class SinglePermitBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_permit", nullable = false)
    private LocalDate dateDebutPermit;

    @Column(name = "date_fin_permit", nullable = false)
    private LocalDate dateFinPermit;

    @Column(name = "region_instruction", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SinglePermitBeRegionEnum regionInstruction;

    @Column(name = "type_activite", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SinglePermitBeTypeActiviteEnum typeActivite;

    @Column(name = "motif_demande", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private SinglePermitBeMotifEnum motifDemande;

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
