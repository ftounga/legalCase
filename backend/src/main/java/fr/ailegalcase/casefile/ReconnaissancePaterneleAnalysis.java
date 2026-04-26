package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-18-01 : entity 1:1 par dossier portant l'analyse de recevabilité d'une
 * reconnaissance paternelle (FR).
 */
@Entity
@Table(name = "reconnaissance_paternelle_analyses")
@Getter
@Setter
public class ReconnaissancePaterneleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "sous_type", nullable = false, length = 50)
    private ReconnaissancePaterneleCalculator.SousType sousType;

    @Column(name = "date_naissance_enfant")
    private LocalDate dateNaissanceEnfant;

    @Column(name = "date_reconnaissance")
    private LocalDate dateReconnaissance;

    @Column(name = "consentement_libre_du_pere", nullable = false)
    private boolean consentementLibreDuPere;

    @Column(name = "paternite_vraisemblable", nullable = false)
    private boolean paterniteVraisemblable;

    @Column(name = "enfant_non_reconnu_par_autre_pere", nullable = false)
    private boolean enfantNonReconnuParAutrePere;

    @Column(name = "procedure_respectee", nullable = false)
    private boolean procedureRespectee;

    @Column(name = "presence_par_procuration", nullable = false)
    private boolean presenceParProcuration;

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
