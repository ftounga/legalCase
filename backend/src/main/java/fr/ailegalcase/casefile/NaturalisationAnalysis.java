package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-IM-13-01 : entity 1:1 par dossier portant l'analyse de naturalisation française
 * (Code civil art. 21-15 et s.). Outil single-country FR.
 */
@Entity
@Table(name = "naturalisation_analyses")
@Getter
@Setter
public class NaturalisationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "voie_naturalisation", nullable = false, length = 40)
    private String voieNaturalisation;

    @Column(name = "duree_residence_reguliere_annees")
    private Integer dureeResidenceReguliereAnnees;

    @Column(name = "duree_mariage_annees")
    private Integer dureeMariageAnnees;

    @Column(name = "cohabitation_continue")
    private Boolean cohabitationContinue;

    @Column(name = "age_demandeur")
    private Integer ageDemandeur;

    @Column(name = "ascendant_direct_francais")
    private Boolean ascendantDirectFrancais;

    @Column(name = "parent_acquiert_nationalite")
    private Boolean parentAcquiertNationalite;

    @Column(name = "vit_avec_parent_acquereur")
    private Boolean vitAvecParentAcquereur;

    @Column(name = "ancien_francais")
    private Boolean ancienFrancais;

    @Column(name = "casier_judiciaire_vierge", nullable = false)
    private boolean casierJudiciaireVierge = true;

    @Column(name = "assimilation_langue_b1")
    private Boolean assimilationLangueB1;

    @Column(name = "ressources_stables")
    private Boolean ressourcesStables;

    @Column(name = "opposition_gouvernementale_active", nullable = false)
    private boolean oppositionGouvernementaleActive = false;

    @Column(name = "etudes_superieures_france")
    private Boolean etudesSuperieuresFrance;

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
