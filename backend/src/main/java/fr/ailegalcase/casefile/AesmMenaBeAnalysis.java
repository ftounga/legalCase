package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-11 : entity 1:1 par dossier portant l'analyse de l'outil composite
 * AESM + tutelle MENA BE — F-IM-30-aesm-mena-be.
 *
 * <p>Outil <b>BELGIQUE UNIQUEMENT</b> — mineurs étrangers non accompagnés (MENA)
 * uniquement (gate {@code ageActuel < 18} dans le service / calculator). Pattern
 * miroir de {@link NaturalisationConjointBelgeBeAnalysis} (SF-215-09) — snapshot JSON
 * complet (inputs + outputs) dans {@code result_data} pour permettre la restitution
 * UI sans recalcul (pattern F-DT-42).
 *
 * <p>Sources : Loi 04/05/2007 relative à la tutelle MENA (Service des Tutelles SPF
 * Justice — volet tutelle DGDE) ; loi 15/12/1980 art. 9bis adapté MENA + circulaire
 * OE 15/09/2005 (volet AESM).
 */
@Entity
@Table(name = "aesm_mena_be_analyses")
@Getter
@Setter
public class AesmMenaBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "age_actuel", nullable = false)
    private Integer ageActuel;

    @Column(name = "date_arrivee_belgique", nullable = false)
    private LocalDate dateArriveeBelgique;

    @Column(name = "tuteur_designe", nullable = false)
    private Boolean tuteurDesigne;

    @Column(name = "integration_scolaire", nullable = false)
    private Boolean integrationScolaire;

    /** Durée de scolarité continue en mois — nullable (requis uniquement si {@code integrationScolaire = true}). */
    @Column(name = "duree_scolaire")
    private Integer dureeScolaire;

    @Column(name = "projet_vie_elabore", nullable = false)
    private Boolean projetVieElabore;

    @Column(name = "perspective_autonomie", nullable = false)
    private Boolean perspectiveAutonomie;

    @Column(name = "menace_ordre_public", nullable = false)
    private Boolean menaceOrdrePublic;

    /** Volet AESM — score 0-100 avec bonus durée scolaire (cap 100). */
    @Column(name = "score_integration", nullable = false)
    private Integer scoreIntegration;

    @Column(name = "verdict_aesm", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AesmMenaBeVerdictAesm verdictAesm;

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
