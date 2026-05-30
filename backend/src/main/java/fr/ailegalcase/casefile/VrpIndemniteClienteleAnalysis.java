package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-11 : entity 1:1 par dossier portant l'analyse de la rupture d'un VRP
 * statutaire (statut, préavis, indemnité de clientèle — art. L.7311-1 et s. CT).
 * Outil single-country FR.
 */
@Entity
@Table(name = "vrp_indemnite_clientele_analyses")
@Getter
@Setter
public class VrpIndemniteClienteleAnalysis {

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
    @Column(name = "cause_rupture", nullable = false, length = 30)
    private VrpCauseRupture causeRupture;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_vrp", nullable = false, length = 20)
    private VrpTypeVrp typeVrp;

    @Column(name = "commissions_annuelles_moyennes", nullable = false, precision = 19, scale = 2)
    private BigDecimal commissionsAnnuellesMoyennes;

    @Column(name = "salaire_mensuel_moyen", nullable = false, precision = 19, scale = 2)
    private BigDecimal salaireMensuelMoyen;

    @Column(name = "clientele_developpee", nullable = false)
    private boolean clienteleDeveloppee;

    @Column(name = "duree_preavis_mois", nullable = false)
    private int dureePreavisMois;

    @Enumerated(EnumType.STRING)
    @Column(name = "eligibilite_clientele", nullable = false, length = 20)
    private VrpEligibiliteClientele eligibiliteClientele;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_recommandee", nullable = false, length = 30)
    private VrpOptionRecommandee optionRecommandee;

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
