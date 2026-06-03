package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-221-03 : entity 1:1 par dossier portant l'analyse d'éligibilité au statut de
 * RÉSIDENT LONGUE DURÉE UE (art. 15bis Loi 15/12/1980 — directive 2003/109/CE).
 * Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link CarteBSejourIllimiteBeAnalysis} : snapshot JSON complet
 * (inputs + outputs) dans {@code result_data} pour permettre la restitution UI
 * sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "residence_longue_duree_ue_be_analyses")
@Getter
@Setter
public class ResidenceLongueDureeUeBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_debut_sejour_legal", nullable = false)
    private LocalDate dateDebutSejourLegal;

    @Column(name = "sejour_legal_ininterrompu", nullable = false)
    private Boolean sejourLegalIninterrompu;

    @Column(name = "ressources_stables_suffisantes", nullable = false)
    private Boolean ressourcesStablesSuffisantes;

    @Column(name = "assurance_maladie", nullable = false)
    private Boolean assuranceMaladie;

    @Column(name = "condition_integration_remplie", nullable = false)
    private Boolean conditionIntegrationRemplie;

    @Column(name = "absences_hors_ue_excessives", nullable = false)
    private Boolean absencesHorsUeExcessives;

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
