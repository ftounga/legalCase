package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-20-01 : entity 1:1 par dossier portant l'analyse de légalité d'une mesure d'éloignement
 * administrative française autre que l'OQTF (CESEDA L.631+, L.612+, L.222+).
 * Outil <b>single-country FR</b>.
 */
@Entity
@Table(name = "mesures_eloignement_analyses")
@Getter
@Setter
public class MesuresEloignementAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "dispositif", nullable = false, length = 40)
    private String dispositif;

    @Column(name = "motif_menace", nullable = false, length = 30)
    private String motifMenace;

    @Column(name = "procedure_commission_respectee")
    private Boolean procedureCommissionRespectee;

    @Column(name = "urgence_absolue_justifiee")
    private Boolean urgenceAbsolueJustifiee;

    @Column(name = "duree_circularite_precaire")
    private Integer dureeCircularitePrecaire;

    @Column(name = "duree_presence_irreguliere_mois")
    private Integer dureePresenceIrreguliereMois;

    @Column(name = "comportement_aggravant")
    private Boolean comportementAggravant;

    @Column(name = "recours_delai")
    private LocalDate recoursDelai;

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
