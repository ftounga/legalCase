package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-20-01 : entity 1:1 par dossier portant l'analyse "dissolution PACS"
 * (art. 515-7 + 515-7-1 + 515-8 Cciv).
 * Outil single-country FRANCE + DROIT_FAMILLE.
 */
@Entity
@Table(name = "pacs_dissolution_analyses")
@Getter
@Setter
public class PacsDissolutionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_conclusion_pacs")
    private LocalDate dateConclusionPacs;

    @Column(name = "mode_dissolution", nullable = false, length = 40)
    private String modeDissolution;

    @Column(name = "date_dissolution")
    private LocalDate dateDissolution;

    @Column(name = "duree_union_annees", nullable = false)
    private int dureeUnionAnnees;

    @Column(name = "regime_biens", nullable = false, length = 40)
    private String regimeBiens;

    @Column(name = "patrimoine_commun_significatif", nullable = false)
    private boolean patrimoineCommunSignificatif;

    @Column(name = "enfants_communs", nullable = false)
    private int enfantsCommuns;

    @Column(name = "date_notification_partenaire")
    private LocalDate dateNotificationPartenaire;

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
