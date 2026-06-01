package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-33 : entity 1:1 par dossier portant l'analyse du statut et de la
 * protection d'un délégué syndical (DS) ou représentant de section syndicale
 * (RSS) (art. L.2143-1 et s., L.2142-1-1, L.2143-3, L.2411-3 CT, F-DT-69).
 * Outil single-country FR.
 */
@Entity
@Table(name = "delegation_syndicale_analyses")
@Getter
@Setter
public class DelegationSyndicaleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "effectif", nullable = false)
    private int effectif;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mandat", nullable = false, length = 20)
    private MandatSyndicalType typeMandat;

    @Column(name = "syndicat_representatif", nullable = false)
    private boolean syndicatRepresentatif;

    @Column(name = "pourcentage_score_personnel")
    private BigDecimal pourcentageScorePersonnel;

    @Column(name = "date_designation")
    private LocalDate dateDesignation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_designation", nullable = false, length = 20)
    private DelegationSyndicaleStatutDesignation statutDesignation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_protege", nullable = false, length = 10)
    private DelegationSyndicaleStatutProtege statutProtege;

    @Column(name = "licenciement_envisage", nullable = false)
    private boolean licenciementEnvisage;

    @Column(name = "autorisation_inspecteur_travail", nullable = false)
    private boolean autorisationInspecteurTravail;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_nullite_licenciement", nullable = false, length = 20)
    private DelegationSyndicaleRisqueNullite risqueNulliteLicenciement;

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
