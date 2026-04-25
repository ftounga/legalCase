package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-13-01 : entity 1:1 par dossier portant l'analyse de risque de requalification d'un
 * licenciement pour motif économique (FR).
 *
 * <p>Les listes (preuvesMotif, criteresOrdreAppliques, tentativesReclassement) sont
 * sérialisées dans {@code result_data} (JSON complet) — les colonnes dédiées portent les
 * scalaires pour faciliter d'éventuels filtres SQL.</p>
 */
@Entity
@Table(name = "licenciement_economique_analyses")
@Getter
@Setter
public class LicenciementEconomiqueAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_economique_invoque", nullable = false, length = 40)
    private LicenciementEconomiqueCalculator.MotifEconomique motifEconomiqueInvoque;

    @Column(name = "preuves_motif_json", nullable = false, columnDefinition = "TEXT")
    private String preuvesMotifJson = "[]";

    @Column(name = "criteres_ordre_json", nullable = false, columnDefinition = "TEXT")
    private String criteresOrdreJson = "[]";

    @Column(name = "salarie_age", nullable = false)
    private int salarieAge;

    @Column(name = "salarie_anciennete_mois", nullable = false)
    private int salarieAncienneteMois;

    @Column(name = "salarie_charges_famille", nullable = false)
    private int salarieChargesFamille;

    @Enumerated(EnumType.STRING)
    @Column(name = "salarie_qualites_prof", length = 20)
    private LicenciementEconomiqueCalculator.QualitesProf salarieQualitesProf;

    @Column(name = "tentatives_reclassement_json", nullable = false, columnDefinition = "TEXT")
    private String tentativesReclassementJson = "[]";

    @Column(name = "priorite_reembauche_propose", nullable = false)
    private boolean prioriteReembauchePropose;

    @Column(name = "conge_reclassement_propose", nullable = false)
    private boolean congeReclassementPropose;

    @Column(name = "date_notification", nullable = false)
    private LocalDate dateNotification;

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
