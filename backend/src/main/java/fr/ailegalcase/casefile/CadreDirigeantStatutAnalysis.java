package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-19 : entity 1:1 par dossier portant l'analyse de qualification du cadre
 * dirigeant (art. L.3111-2 CT — 3 critères cumulatifs, F-DT-107). Outil
 * single-country FR.
 */
@Entity
@Table(name = "cadre_dirigeant_statut_analyses")
@Getter
@Setter
public class CadreDirigeantStatutAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "independance_emploi_du_temps", nullable = false)
    private boolean independanceEmploiDuTemps;

    @Column(name = "autonomie_decision", nullable = false)
    private boolean autonomieDecision;

    @Column(name = "remuneration_parmi_plus_elevees", nullable = false)
    private boolean remunerationParmiPlusElevees;

    @Column(name = "participation_direction_entreprise", nullable = false)
    private boolean participationDirectionEntreprise;

    @Column(name = "niveau_remuneration_constate")
    private BigDecimal niveauRemunerationConstate;

    @Column(name = "criteres_remplis", nullable = false)
    private int criteresRemplis;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualification", nullable = false, length = 30)
    private CadreDirigeantQualification qualification;

    @Enumerated(EnumType.STRING)
    @Column(name = "exclusion_duree_travail", nullable = false, length = 20)
    private CadreDirigeantExclusionDureeTravail exclusionDureeTravail;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_rappel_heures_supp", nullable = false, length = 20)
    private CadreDirigeantRisqueRappelHeuresSupp risqueRappelHeuresSupp;

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
