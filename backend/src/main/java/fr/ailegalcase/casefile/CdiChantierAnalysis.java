package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-25 : entity 1:1 par dossier portant l'analyse du licenciement pour fin
 * de chantier du CDI de chantier / d'opération (art. L.1223-8 et s. ; L.1236-8
 * CT, F-DT-37). Outil single-country FR.
 */
@Entity
@Table(name = "cdi_chantier_analyses")
@Getter
@Setter
public class CdiChantierAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_entree", nullable = false)
    private LocalDate dateEntree;

    @Column(name = "date_rupture", nullable = false)
    private LocalDate dateRupture;

    @Enumerated(EnumType.STRING)
    @Column(name = "fondement_recours", nullable = false, length = 30)
    private CdiChantierFondementRecours fondementRecours;

    @Enumerated(EnumType.STRING)
    @Column(name = "secteur", nullable = false, length = 20)
    private CdiChantierSecteur secteur;

    @Column(name = "chantier_acheve", nullable = false)
    private boolean chantierAcheve;

    @Column(name = "salaire_mensuel_moyen", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuelMoyen;

    @Column(name = "reclassement_autre_chantier_propose", nullable = false)
    private boolean reclassementAutreChantierPropose;

    @Column(name = "anciennete_annees", nullable = false)
    private int ancienneteAnnees;

    @Column(name = "recours_valide", nullable = false)
    private boolean recoursValide;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_licenciement", nullable = false, length = 30)
    private CdiChantierMotifLicenciement motifLicenciement;

    @Column(name = "indemnite_licenciement", nullable = false, precision = 14, scale = 2)
    private BigDecimal indemniteLicenciement;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_global", nullable = false, length = 30)
    private CdiChantierVerdict verdictGlobal;

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
