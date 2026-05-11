package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-211-02 : entity 1:1 par dossier portant l'analyse divorce DDI 3 voies belge.
 * CC art. 229 §§1, 2, 3 + Loi 27/04/2007. Outil single-country BE.
 */
@Entity
@Table(name = "divorce_ddi_be_analyses")
@Getter
@Setter
public class DivorceDdiBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_separation", nullable = false)
    private LocalDate dateSeparation;

    @Column(name = "nature_demande", nullable = false, length = 40)
    private String natureDemande;

    @Column(name = "preuves_desunion_disponibles", nullable = false)
    private boolean preuvesDesunionDisponibles;

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
