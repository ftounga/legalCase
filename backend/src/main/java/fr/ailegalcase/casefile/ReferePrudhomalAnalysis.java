package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-34-01 : entity 1:1 par dossier portant l'analyse du référé prud'homal FR
 * (R.1454-1 et s. Code travail). Outil single-country FRANCE.
 */
@Entity
@Table(name = "refere_prudhomal_analyses")
@Getter
@Setter
public class ReferePrudhomalAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "type_refere", nullable = false, length = 32)
    private String typeRefere;

    @Column(name = "nature_creance", nullable = false, length = 32)
    private String natureCreance;

    @Column(name = "montant_provision_demandee_eur", precision = 12, scale = 2)
    private BigDecimal montantProvisionDemandeeEur;

    @Column(name = "absence_contestation_serieuse", nullable = false)
    private boolean absenceContestationSerieuse;

    @Column(name = "preuves_urgence_produites", columnDefinition = "TEXT")
    private String preuvesUrgenceProduitesJson;

    @Column(name = "dommage_immediat_carac", nullable = false)
    private boolean dommageImmediatCarac;

    @Column(name = "tresorerie_employeur_douteuse", nullable = false)
    private boolean tresorerieEmployeurDouteuse;

    @Column(name = "date_mise_en_demeure")
    private LocalDate dateMiseEnDemeure;

    @Column(name = "anciennete_contrat_mois")
    private Integer ancienneteContratMois;

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
