package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-19-05 : entity 1:1 par dossier portant l'analyse d'éligibilité au
 * recours JAF en cas de désaccord parental (art. 373-2-10 Cciv).
 * Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "desaccords_parentaux_analyses")
@Getter
@Setter
public class DesaccordsParentauxAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "domaine_desaccord", length = 40, nullable = false)
    private String domaineDesaccord;

    @Column(name = "intensite_desaccord", length = 20, nullable = false)
    private String intensiteDesaccord;

    @Column(name = "tentatives_mediation", nullable = false, columnDefinition = "TEXT")
    private String tentativesMediation = "[]";

    @Column(name = "age_enfants_concernes", nullable = false, columnDefinition = "TEXT")
    private String ageEnfantsConcernes = "[]";

    @Column(name = "interet_superieur_invoque", nullable = false)
    private boolean interetSuperieurInvoque;

    @Column(name = "expertise_deja_realisee", nullable = false)
    private boolean expertiseDejaRealisee;

    @Column(name = "urgence", nullable = false)
    private boolean urgence;

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
