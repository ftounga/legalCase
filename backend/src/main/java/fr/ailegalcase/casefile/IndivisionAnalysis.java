package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-22-01 : entity 1:1 par dossier portant l'analyse d'éligibilité au
 * partage judiciaire d'une indivision post-communautaire (art. 815 Cciv +
 * art. 1364 CPC). Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "indivision_analyses")
@Getter
@Setter
public class IndivisionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_origine_indivision", nullable = false)
    private LocalDate dateOrigineIndivision;

    /** CSV des enums natureBiens (sérialisation simple, lecture côté result_data). */
    @Column(name = "nature_biens", length = 200, nullable = false)
    private String natureBiens;

    @Column(name = "valeur_estimee_totale_eur", precision = 14, scale = 2, nullable = false)
    private BigDecimal valeurEstimeeTotaleEur;

    @Column(name = "nb_indivisaires", nullable = false)
    private int nbIndivisaires;

    /** CSV des enums tentativesPartageAmiable. */
    @Column(name = "tentatives_partage_amiable", length = 255, nullable = false)
    private String tentativesPartageAmiable;

    @Column(name = "consentement_partage_global", nullable = false)
    private boolean consentementPartageGlobal;

    @Column(name = "occupation_bien_par_un_indivisaire", nullable = false)
    private boolean occupationBienParUnIndivisaire;

    @Column(name = "indivision_duree_annees", nullable = false)
    private int indivisionDureeAnnees;

    @Column(name = "demande_mesures_conservatoires", nullable = false)
    private boolean demandeMesuresConservatoires;

    @Column(name = "conflit_ouvert_entre_indivisaires", nullable = false)
    private boolean conflitOuvertEntreIndivisaires;

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
