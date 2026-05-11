package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-211-03 : entity 1:1 par dossier portant l'analyse des mesures provisoires
 * Tribunal de la famille belge (CJ art. 1280). Outil single-country BE.
 */
@Entity
@Table(name = "tribunal_famille_be_mesures_prov_analyses")
@Getter
@Setter
public class TribunalFamilleBeMesuresProvisoiresAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "violence_familiale", nullable = false)
    private boolean violenceFamiliale;

    @Column(name = "deplacement_enfant_imminent", nullable = false)
    private boolean deplacementEnfantImminent;

    @Column(name = "dilapidation_patrimoine", nullable = false)
    private boolean dilapidationPatrimoine;

    @Column(name = "besoin_residence_separee", nullable = false)
    private boolean besoinResidenceSeparee;

    @Column(name = "besoin_contribution_alimentaire", nullable = false)
    private boolean besoinContributionAlimentaire;

    @Column(name = "besoin_autorite_parentale_exclusive", nullable = false)
    private boolean besoinAutoriteParentaleExclusive;

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
