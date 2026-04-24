package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-09-01 : entity 1:1 par dossier portant l'analyse AES métier en tension
 * (art. 26 loi 26/01/2024 + L.435-4 CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "aes_metiers_tension_analyses")
@Getter
@Setter
public class AesMetiersTensionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_entree_france", nullable = false)
    private LocalDate dateEntreeFrance;

    @Column(name = "mois_activite_salariee_dernieres_24_mois", nullable = false)
    private int moisActiviteSalarieeDernieres24Mois;

    @Column(name = "metier_est_en_tension", nullable = false)
    private boolean metierEstEnTension;

    @Column(name = "code_metier", length = 20)
    private String codeMetier;

    @Column(name = "menace_ordre_public", nullable = false)
    private boolean menaceOrdrePublic;

    @Column(name = "contrat_ou_promesse_valide", nullable = false)
    private boolean contratOuPromesseValide;

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
