package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-220-03 : entity 1:1 par dossier portant l'analyse d'éligibilité d'un jeune
 * majeur (16-21 ans, ex-MNA scolarisé) à la carte VPF de l'art. L.423-22 CESEDA.
 * Outil single-country FR (F-IM-49-vpf-jeune-majeur-l42322-fr).
 */
@Entity
@Table(name = "vpf_jeune_majeur_analyses")
@Getter
@Setter
public class VpfJeuneMajeurAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "age", nullable = false)
    private int age;

    @Column(name = "entre_mineur", nullable = false)
    private boolean entreMineur;

    @Column(name = "date_entree_france")
    private LocalDate dateEntreeFrance;

    @Column(name = "age_entree_ase")
    private Integer ageEntreeAse;

    @Column(name = "prise_en_charge_ase", nullable = false)
    private boolean priseEnChargeAse;

    @Column(name = "date_debut_prise_en_charge")
    private LocalDate dateDebutPriseEnCharge;

    @Column(name = "anciennete_mois_prise_en_charge")
    private Integer ancienneteMoisPriseEnCharge;

    @Column(name = "scolarise_ou_formation", nullable = false)
    private boolean scolariseOuFormation;

    @Column(name = "caractere_reel_serieux_formation", nullable = false)
    private boolean caractereReelEtSerieuxFormation;

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
