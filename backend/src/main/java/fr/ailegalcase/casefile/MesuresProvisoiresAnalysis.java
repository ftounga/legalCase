package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-12-01 : entity 1:1 par dossier portant l'analyse des mesures provisoires
 * (audience AOMP, art. 254-256 Cciv). Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "mesures_provisoires_analyses")
@Getter
@Setter
public class MesuresProvisoiresAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_audience_aomp", nullable = false)
    private LocalDate dateAudienceAOMP;

    @Column(name = "revenus_demandeur_eur", precision = 12, scale = 2, nullable = false)
    private BigDecimal revenusEpouxDemandeurEur;

    @Column(name = "revenus_defendeur_eur", precision = 12, scale = 2, nullable = false)
    private BigDecimal revenusEpouxDefendeurEur;

    @Column(name = "logement_proprietaire", length = 30, nullable = false)
    private String logementProprietaire;

    @Column(name = "souhait_residence_enfants", length = 30, nullable = false)
    private String souhaitResidenceEnfants;

    @Column(name = "violences_alleguees", nullable = false)
    private boolean violencesAlleguees;

    @Column(name = "patrimoine_commun_significatif", nullable = false)
    private boolean patrimoineCommunIsignificatif;

    @Column(name = "demande_mesure_conservatoire", nullable = false)
    private boolean demandeMesureConservatoire;

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
