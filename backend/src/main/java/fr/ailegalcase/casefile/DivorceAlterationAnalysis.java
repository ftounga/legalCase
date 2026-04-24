package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-08-01 : entity 1:1 par dossier portant l'analyse du divorce pour
 * altération définitive du lien conjugal (art. 237-238 Cciv). Outil
 * single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "divorce_alteration_analyses")
@Getter
@Setter
public class DivorceAlterationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_cessation_vie_commune", nullable = false)
    private LocalDate dateCessationVieCommune;

    @Column(name = "preuves_separation_documentaires", nullable = false)
    private boolean preuvesSeparationDocumentaires;

    @Column(name = "tentatives_reconciliation", nullable = false)
    private boolean tentativesReconciliation;

    @Column(name = "duree_mariage_annees", nullable = false)
    private int dureeMariageAnnees;

    @Column(name = "revenus_annuels_epoux1_eur", precision = 12, scale = 2)
    private BigDecimal revenusAnnuelsEpoux1Eur;

    @Column(name = "revenus_annuels_epoux2_eur", precision = 12, scale = 2)
    private BigDecimal revenusAnnuelsEpoux2Eur;

    @Column(name = "patrimoine_commun_significatif", nullable = false)
    private boolean patrimoineCommunSignificatif;

    @Column(name = "date_assignation")
    private LocalDate dateAssignation;

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
