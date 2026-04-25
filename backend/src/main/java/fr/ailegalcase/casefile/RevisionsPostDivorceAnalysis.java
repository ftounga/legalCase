package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-13-01 : entity 1:1 par dossier portant l'analyse révision post-divorce FR
 * (art. 209 / 373-2-13 / 373-2-9 / 279 / 373-2 Cciv). Outil single-country FR.
 */
@Entity
@Table(name = "revisions_post_divorce_analyses")
@Getter
@Setter
public class RevisionsPostDivorceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "type_revision", nullable = false, length = 40)
    private String typeRevision;

    @Column(name = "date_decision_initiale")
    private LocalDate dateDecisionInitiale;

    @Column(name = "changement_circonstance", columnDefinition = "TEXT")
    private String changementCirconstance;

    @Column(name = "revenus_initials_debiteur_eur", precision = 15, scale = 2)
    private BigDecimal revenusInitialsDebiteurEur;

    @Column(name = "revenus_actuels_debiteur_eur", precision = 15, scale = 2)
    private BigDecimal revenusActuelsDebiteurEur;

    @Column(name = "revenus_initials_creancier_eur", precision = 15, scale = 2)
    private BigDecimal revenusInitialsCreancierEur;

    @Column(name = "revenus_actuels_creancier_eur", precision = 15, scale = 2)
    private BigDecimal revenusActuelsCreancierEur;

    @Column(name = "nb_enfants_a_charge")
    private Integer nbEnfantsACharge;

    /** JSON array stringifié des âges des enfants. */
    @Column(name = "age_enfants", columnDefinition = "TEXT")
    private String ageEnfants;

    @Column(name = "mode_residence_actuel", length = 30)
    private String modeResidenceActuel;

    @Column(name = "mode_residence_demande", length = 30)
    private String modeResidenceDemande;

    @Column(name = "information_prealable")
    private Boolean informationPrealable;

    @Column(name = "distance_demenagement_km")
    private Integer distanceDemenagementKm;

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
