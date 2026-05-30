package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-09 : entity 1:1 par dossier portant l'analyse de recevabilité d'une
 * action de groupe en discrimination au travail (art. L. 1134-7 à L. 1134-10
 * Code travail). Outil single-country FR.
 */
@Entity
@Table(name = "action_groupe_discrimination_analyses")
@Getter
@Setter
public class ActionGroupeDiscriminationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_organisation", nullable = false, length = 30)
    private ActionGroupeDiscriminationTypeOrganisation typeOrganisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_discrimination", nullable = false, length = 30)
    private ActionGroupeDiscriminationMotif motifDiscrimination;

    @Column(name = "nombre_personnes_concernees", nullable = false)
    private int nombrePersonnesConcernees;

    @Enumerated(EnumType.STRING)
    @Column(name = "objet_action", nullable = false, length = 30)
    private ActionGroupeDiscriminationObjet objetAction;

    @Column(name = "date_mise_en_demeure")
    private LocalDate dateMiseEnDemeure;

    @Column(name = "qualite_a_agir", nullable = false)
    private boolean qualiteAAgir;

    @Column(name = "pluralite_etablie", nullable = false)
    private boolean pluraliteEtablie;

    @Column(name = "date_recevabilite_saisine")
    private LocalDate dateRecevabiliteSaisine;

    @Column(name = "delai_carence_respecte", nullable = false)
    private boolean delaiCarenceRespecte;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private ActionGroupeDiscriminationVerdict verdict;

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
