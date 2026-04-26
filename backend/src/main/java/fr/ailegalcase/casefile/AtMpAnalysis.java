package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-33-01 : entity 1:1 par dossier portant l'analyse de recevabilité d'une procédure
 * AT/MP française (CSS L.411-1 / L.461-1 / L.434-2).
 * Outil <b>single-country FR</b>.
 */
@Entity
@Table(name = "at_mp_analyses")
@Getter
@Setter
public class AtMpAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "dispositif", nullable = false, length = 40)
    private String dispositif;

    @Column(name = "date_accident")
    private LocalDate dateAccident;

    @Column(name = "lieu_travail")
    private Boolean lieuTravail;

    @Column(name = "declaration_employeur_dans_les_48h")
    private Boolean declarationEmployeurDansLes48h;

    @Column(name = "certificat_medical_initial")
    private Boolean certificatMedicalInitial;

    @Column(name = "numero_tableau", length = 30)
    private String numeroTableau;

    @Column(name = "delai_prise_en_charge_respecte")
    private Boolean delaiPriseEnChargeRespecte;

    @Column(name = "date_exposition")
    private LocalDate dateExposition;

    @Column(name = "taux_fixe_par_cpam")
    private Integer tauxFixeParCpam;

    @Column(name = "taux_revendique")
    private Integer tauxRevendique;

    @Column(name = "expertise_medicale_produite")
    private Boolean expertiseMedicaleProduite;

    @Column(name = "date_premier_avis_cpam")
    private LocalDate datePremierAvisCpam;

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
