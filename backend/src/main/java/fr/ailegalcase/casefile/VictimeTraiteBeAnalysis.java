package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-221-06 : entity 1:1 par dossier portant l'analyse du titre de séjour « victime de la
 * traite des êtres humains » (BE — art. 61/2 et s. Loi 15/12/1980 ; circulaire du
 * 26/09/2008). Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Snapshot JSON complet (inputs + outputs) dans {@code result_data} pour permettre la
 * restitution UI sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "victime_traite_be_analyses")
@Getter
@Setter
public class VictimeTraiteBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "phase_procedure", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private VictimeTraiteBePhase phaseProcedure;

    @Column(name = "rupture_avec_reseau", nullable = false)
    private Boolean ruptureAvecReseau;

    @Column(name = "cooperation_judiciaire", nullable = false)
    private Boolean cooperationJudiciaire;

    @Column(name = "accompagnement_centre_specialise", nullable = false)
    private Boolean accompagnementCentreSpecialise;

    @Column(name = "date_debut_accompagnement")
    private LocalDate dateDebutAccompagnement;

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
