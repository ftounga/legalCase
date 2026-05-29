package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-25 : entity 1:1 par dossier portant l'analyse des démarches ANEF et des
 * recours en cas de panne du dépôt dématérialisé (R. 311-2-2 CESEDA). Outil
 * single-country FR.
 */
@Entity
@Table(name = "anef_procedure_analyses")
@Getter
@Setter
public class AnefProcedureAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "type_titre_concerne", length = 120)
    private String typeTitreConcerne;

    @Column(name = "date_expiration_titre", nullable = false)
    private LocalDate dateExpirationTitre;

    @Column(name = "pannee_anef_signalee", nullable = false)
    private boolean panneeANEFSignalee;

    @Column(name = "date_tentative_depot")
    private LocalDate dateTentativeDepot;

    @Column(name = "demande_adressee_prefecture", nullable = false)
    private boolean demandeAdresseePrefecture;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private AnefProcedureStatut statut;

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
