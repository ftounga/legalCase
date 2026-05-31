package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-27 : entity 1:1 par dossier portant l'analyse de conformité de la
 * procédure interne de traitement d'un signalement de harcèlement (art.
 * L.1153-5-1, L.2314-1, L.1152-4, L.4121-1 CT, F-DT-59). Outil single-country FR.
 */
@Entity
@Table(name = "harcelement_procedure_interne_analyses")
@Getter
@Setter
public class HarcelementProcedureInterneAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "effectif", nullable = false)
    private int effectif;

    @Column(name = "signalement_recu", nullable = false)
    private boolean signalementRecu;

    @Column(name = "delai_reaction_jours")
    private Integer delaiReactionJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "delai_raisonnable", nullable = false, length = 10)
    private HarcelementProcedureInterneDelaiRaisonnable delaiRaisonnable;

    @Column(name = "items_obligatoires_manquants", nullable = false)
    private int itemsObligatoiresManquants;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private HarcelementProcedureInterneVerdict statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_responsabilite_employeur", nullable = false, length = 10)
    private HarcelementProcedureInterneRisque risqueResponsabiliteEmployeur;

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
