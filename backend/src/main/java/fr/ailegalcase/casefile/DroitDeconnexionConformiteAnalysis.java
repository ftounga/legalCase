package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-53 : entity 1:1 par dossier portant l'analyse de conformité à
 * l'obligation relative au droit à la déconnexion (art. L.2242-17 7° CT,
 * F-DT-83). Outil single-country FR.
 */
@Entity
@Table(name = "droit_deconnexion_conformite_analyses")
@Getter
@Setter
public class DroitDeconnexionConformiteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "effectif", nullable = false)
    private int effectif;

    @Column(name = "delegue_syndical_present", nullable = false)
    private boolean delegueSyndicalPresent;

    @Column(name = "accord_ou_charte_present", nullable = false)
    private boolean accordOuChartePresent;

    @Column(name = "plages_deconnexion_definies", nullable = false)
    private boolean plagesDeconnexionDefinies;

    @Column(name = "actions_sensibilisation", nullable = false)
    private boolean actionsSensibilisation;

    @Column(name = "avis_cse_recueilli_pour_charte", nullable = false)
    private boolean avisCseRecueilliPourCharte;

    @Column(name = "obligation_de_negocier", nullable = false)
    private boolean obligationDeNegocier;

    @Column(name = "items_manquants", nullable = false)
    private int itemsManquants;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 30)
    private DroitDeconnexionConformiteStatut statut;

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
