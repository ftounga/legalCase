package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-IM-17-01 : entité 1:1 par dossier portant l'analyse du régime franco-algérien
 * (accord du 27/12/1968 + avenants 1985 / 1994 / 2001). Outil single-country FRANCE.
 */
@Entity
@Table(name = "regime_algerien_analyses")
@Getter
@Setter
public class RegimeAlgerienAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "voie_demande", nullable = false, length = 60)
    private String voieDemande;

    @Column(name = "nationalite_algerienne", nullable = false)
    private boolean nationaliteAlgerienne = true;

    @Column(name = "document_etat_civil_original")
    private Boolean documentEtatCivilOriginal;

    @Column(name = "presence_reguliere_france_mois")
    private Integer presenceReguliereFranceMois;

    @Column(name = "casier_judiciaire_vierge", nullable = false)
    private boolean casierJudiciaireVierge = true;

    @Column(name = "visa_long_sejour_valide")
    private Boolean visaLongSejourValide;

    @Column(name = "conjoint_francais")
    private Boolean conjointFrancais;

    @Column(name = "parent_enfant_francais")
    private Boolean parentEnfantFrancais;

    @Column(name = "ne_en_france")
    private Boolean neEnFrance;

    @Column(name = "arrivee_avant_13_ans")
    private Boolean arriveeAvant13Ans;

    @Column(name = "contrat_travail_valide")
    private Boolean contratTravailValide;

    @Column(name = "ressources_suffisantes")
    private Boolean ressourcesSuffisantes;

    @Column(name = "logement_decent")
    private Boolean logementDecent;

    @Column(name = "nombre_personnes_foyer")
    private Integer nombrePersonnesFoyer;

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
