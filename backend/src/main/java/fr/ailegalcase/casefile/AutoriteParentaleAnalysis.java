package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-19-01 : entity 1:1 par dossier portant l'analyse d'éligibilité au
 * changement de régime d'exercice de l'autorité parentale (art. 372-373 Cciv).
 * Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "autorite_parentale_analyses")
@Getter
@Setter
public class AutoriteParentaleAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "regime_exercice_actuel", length = 40, nullable = false)
    private String regimeExerciceActuel;

    @Column(name = "regime_exercice_demande", length = 40, nullable = false)
    private String regimeExerciceDemande;

    @Column(name = "motif_changement", length = 40, nullable = false)
    private String motifChangement;

    @Column(name = "danger_caracterise", nullable = false)
    private boolean dangerCaracterise;

    @Column(name = "consentement_autre_parent", nullable = false)
    private boolean consentementAutreParent;

    @Column(name = "interference_vie_enfant", nullable = false)
    private boolean interferenceVieEnfant;

    @Column(name = "date_requete", nullable = false)
    private LocalDate dateRequete;

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
