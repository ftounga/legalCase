package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-43 : entity 1:1 par dossier portant l'analyse des obligations de
 * l'employeur recrutant un travailleur étranger hors UE (autorisation de travail,
 * L. 5221-1 Code du travail). Outil single-country FR.
 */
@Entity
@Table(name = "autorisation_travail_employeur_analyses")
@Getter
@Setter
public class AutorisationTravailEmployeurAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_contrat", nullable = false, length = 20)
    private AutorisationTravailEmployeurTypeContrat typeContrat;

    @Column(name = "poste_proposes", nullable = false, length = 200)
    private String posteProposes;

    @Column(name = "nationalite_candidat", nullable = false, length = 100)
    private String nationaliteCandidat;

    @Column(name = "duree_contrat_mois")
    private Integer dureeContratMois;

    @Column(name = "refus_autorisation", nullable = false)
    private boolean refusAutorisation;

    @Column(name = "date_refus_autorisation")
    private LocalDate dateRefusAutorisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private AutorisationTravailEmployeurStatut statut;

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
