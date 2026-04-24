package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-14-04 : entity 1:1 par dossier portant l'analyse 40ter familial Belge
 * BE (Loi 15/12/1980 art. 40ter — regroupement familial d'un Belge). Outil
 * single-country BE.
 */
@Entity
@Table(name = "belgian_40ter_analyses")
@Getter
@Setter
public class Belgian40terAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "lien_familial", nullable = false, length = 40)
    private String lienFamilial;

    @Column(name = "regroupant_belge", nullable = false)
    private boolean regroupantBelge;

    @Column(name = "revenus_mensuels_nets_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal revenusMensuelsNetsEur;

    @Column(name = "seuil_120_pct_ris_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal seuil120PctRisEur;

    @Column(name = "assurance_maladie", nullable = false)
    private boolean assuranceMaladie;

    @Column(name = "logement_suffisant", nullable = false)
    private boolean logementSuffisant;

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
