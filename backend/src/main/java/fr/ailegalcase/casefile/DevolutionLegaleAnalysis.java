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
import java.util.UUID;

/**
 * SF-FA-24-01 : entity 1:1 par dossier portant l'analyse de dévolution légale
 * successorale (FR — art. 731 et s. Cciv).
 */
@Entity
@Table(name = "devolution_legale_analyses")
@Getter
@Setter
public class DevolutionLegaleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "conjoint_survivant", nullable = false)
    private boolean conjointSurvivant;

    @Column(name = "nb_descendants", nullable = false)
    private int nbDescendants;

    @Column(name = "tous_descendants_communs", nullable = false)
    private boolean tousDescendantsCommunsAvecConjoint;

    @Column(name = "nb_descendants_predecedes", nullable = false)
    private int nbDescendantsPredecedes;

    @Column(name = "nb_petits_enfants_representation", nullable = false)
    private int nbPetitsEnfantsParRepresentation;

    @Column(name = "pere_vivant", nullable = false)
    private boolean pereVivant;

    @Column(name = "mere_vivante", nullable = false)
    private boolean mereVivant;

    @Column(name = "nb_freres_soeurs", nullable = false)
    private int nbFreresSoeurs;

    @Column(name = "nb_freres_soeurs_predecedes", nullable = false)
    private int nbFreresSoeursPredecedes;

    @Column(name = "ascendants_ordinaires", nullable = false)
    private boolean ascendantsOrdinaires;

    @Column(name = "collateral_ordinaires", nullable = false)
    private boolean collateralOrdinaires;

    @Enumerated(EnumType.STRING)
    @Column(name = "option_conjoint", length = 16)
    private DevolutionLegaleCalculator.OptionConjoint optionConjoint;

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
