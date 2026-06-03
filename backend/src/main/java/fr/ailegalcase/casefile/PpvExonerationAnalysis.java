package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-39 : entity 1:1 par dossier portant l'analyse d'exonération de la prime
 * de partage de la valeur (PPV — loi n° 2022-1158 du 16/08/2022 art. 1 + loi
 * n° 2023-1107 du 29/11/2023, F-DT-52). Outil single-country FR.
 */
@Entity
@Table(name = "ppv_exoneration_analyses")
@Getter
@Setter
public class PpvExonerationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "montant_prime", nullable = false, precision = 14, scale = 2)
    private BigDecimal montantPrime;

    @Column(name = "accord_interessement_present", nullable = false)
    private boolean accordInteressementPresent;

    @Column(name = "remuneration_annuelle_brute", nullable = false, precision = 14, scale = 2)
    private BigDecimal remunerationAnnuelleBrute;

    @Column(name = "effectif_moins_50", nullable = false)
    private boolean effectifMoins50;

    @Column(name = "versement_plan_epargne", nullable = false)
    private boolean versementPlanEpargne;

    @Column(name = "plafond_social_applique", nullable = false, precision = 14, scale = 2)
    private BigDecimal plafondSocialApplique;

    @Column(name = "montant_exonere", nullable = false, precision = 14, scale = 2)
    private BigDecimal montantExonere;

    @Column(name = "montant_imposable", nullable = false, precision = 14, scale = 2)
    private BigDecimal montantImposable;

    @Column(name = "exoneration_fiscale_ir", nullable = false)
    private boolean exonerationFiscaleIr;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private PpvExonerationStatut statut;

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
