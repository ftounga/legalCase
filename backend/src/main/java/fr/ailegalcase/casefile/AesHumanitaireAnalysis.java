package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-09-03 : entity 1:1 par dossier portant l'analyse AES voie humanitaire
 * (art. L.435-2 CESEDA + L.432-14 commission titre de séjour). Outil
 * single-country FR.
 */
@Entity
@Table(name = "aes_humanitaire_analyses")
@Getter
@Setter
public class AesHumanitaireAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_entree_france", nullable = false)
    private LocalDate dateEntreeFrance;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_humanitaire_dominant", nullable = false, length = 50)
    private MotifHumanitaire motifHumanitaireDominant;

    @Column(name = "preuves_medicales", nullable = false)
    private boolean preuvesMedicales;

    @Column(name = "preuves_violences_ou_traite", nullable = false)
    private boolean preuvesViolencesOuTraite;

    @Column(name = "demande_asile_deposee_et_rejetee", nullable = false)
    private boolean demandeAsileDeposeeEtRejetee;

    @Column(name = "commission_titre_sejour_saisie", nullable = false)
    private boolean commissionTitreSejourSaisie;

    @Column(name = "menace_ordre_public", nullable = false)
    private boolean menaceOrdrePublic;

    @Column(name = "date_depot_demande")
    private LocalDate dateDepotDemande;

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
