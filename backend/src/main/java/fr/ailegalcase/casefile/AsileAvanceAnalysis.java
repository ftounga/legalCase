package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-12-01 : entity 1:1 par dossier portant l'analyse d'asile avancé
 * (CESEDA Livre V — France). Outil single-country FR.
 */
@Entity
@Table(name = "asile_avance_analyses")
@Getter
@Setter
public class AsileAvanceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "dispositif_asile", nullable = false, length = 40)
    private String dispositifAsile;

    @Column(name = "date_decision_anterieure")
    private LocalDate dateDecisionAnterieure;

    @Column(name = "elements_nouveaux")
    private Boolean elementsNouveaux;

    @Column(name = "pays_origine_dans_liste_surs")
    private Boolean paysOrigineDansListeSurs;

    @Column(name = "empreintes_eurodac_autres_em")
    private Boolean empreintesEurodacAutresEm;

    @Column(name = "demandeur_en_fuite")
    private Boolean demandeurEnFuite;

    @Column(name = "motifs_exclusion")
    private Boolean motifsExclusion;

    @Column(name = "traitements_graves_etablis")
    private Boolean traitementsGravesEtablis;

    @Column(name = "fraude_documentaire_avere")
    private Boolean fraudeDocumentaireAvere;

    @Column(name = "refus_prise_empreintes")
    private Boolean refusPriseEmpreintes;

    @Column(name = "presence_reguliere")
    private Boolean presenceReguliere;

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
