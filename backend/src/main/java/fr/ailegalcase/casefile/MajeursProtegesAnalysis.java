package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-25-01 : entity 1:1 par dossier portant l'analyse d'éligibilité à une
 * mesure de protection des majeurs (art. 433-441 + 494-1 et s. Cciv).
 * Outil single-country FR (DROIT_FAMILLE).
 */
@Entity
@Table(name = "majeurs_proteges_analyses")
@Getter
@Setter
public class MajeursProtegesAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "regime_protection_demande", length = 40, nullable = false)
    private String regimeProtectionDemande;

    @Column(name = "alteration_facultes_mentales", nullable = false)
    private boolean altertationFacultesMentales;

    @Column(name = "alteration_facultes_physiques", nullable = false)
    private boolean altertationFacultesPhysiques;

    @Column(name = "certificat_medical_circonstancie", nullable = false)
    private boolean certificatMedicalCirconstancie;

    @Column(name = "date_certificat_medical")
    private LocalDate dateCertificatMedical;

    @Column(name = "consentement_personne_a_proteger", nullable = false)
    private boolean consentementPersonneAProteger;

    @Column(name = "demandeur_familial", length = 30, nullable = false)
    private String demandeurFamilial;

    @Column(name = "actes_envisages", nullable = false, columnDefinition = "TEXT")
    private String actesEnvisages = "[]";

    @Column(name = "urgence_patrimoniale", nullable = false)
    private boolean urgencePatrimoniale;

    @Column(name = "patrimoine_significatif", nullable = false)
    private boolean patrimoineSignificatif;

    @Column(name = "isolement_social", nullable = false)
    private boolean isolementSocial;

    /**
     * SF-FA-25-03 : critère pivot art. 472 Cciv pour curatelle renforcée.
     * Nullable pour rester compatible avec les analyses persistées avant la
     * migration 159 (où la colonne n'existait pas).
     */
    @Column(name = "incapacite_gestion_quotidienne")
    private Boolean incapaciteGestionQuotidienne;

    /**
     * SF-FA-25-04 : critère pivot art. 440 al. 3 Cciv pour tutelle.
     * true = la personne ne peut plus pourvoir seule à ses intérêts dans les
     * actes essentiels (altération grave et durable). Nullable pour rester
     * compatible avec les analyses persistées avant la migration 160.
     */
    @Column(name = "altertation_grave")
    private Boolean altertationGrave;

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
