package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-29-01 : entity 1:1 par dossier portant l'analyse du crédit-temps belge
 * (CCT 103 + AR 29/10/1997). Trois régimes : AVEC_MOTIF, SANS_MOTIF, FIN_CARRIERE.
 */
@Entity
@Table(name = "credit_temps_be_analyses")
@Getter
@Setter
public class CreditTempsBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "regime", nullable = false, length = 30)
    private String regime;

    @Column(name = "motif", length = 30)
    private String motif;

    @Column(name = "anciennete_entreprise_mois", nullable = false)
    private int ancienneteEntrepriseMois;

    @Column(name = "taille_entreprise_etp", nullable = false)
    private int tailleEntrepriseEtp;

    @Column(name = "duree_reduction_type", nullable = false, length = 30)
    private String dureeReductionType;

    @Column(name = "age_demandeur_annees", nullable = false)
    private int ageDemandeurAnnees;

    @Column(name = "date_demande", nullable = false)
    private LocalDate dateDemande;

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
