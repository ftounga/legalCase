package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-26-01 : entity 1:1 par dossier portant l'analyse d'indemnite compensatrice
 * de conges payes (art. L.3141-26 et L.3141-28 Code du travail).
 */
@Entity
@Table(name = "indemnite_conges_payes_analyses")
@Getter
@Setter
public class IndemniteCongesPayesAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "total_remuneration_periode_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRemunerationPeriodeEur;

    @Column(name = "jours_acquis_annee", nullable = false)
    private int joursAcquisAnnee;

    @Column(name = "jours_pris", nullable = false)
    private int joursPris;

    @Column(name = "salaire_mensuel_brut_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuelBrutEur;

    @Column(name = "date_rupture")
    private LocalDate dateRupture;

    @Column(name = "methode_forcee", length = 20)
    private String methodeForcee;

    @Column(name = "methode_retenue", length = 20, nullable = false)
    private String methodeRetenue;

    @Column(name = "montant_indemnite_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal montantIndemniteEur;

    @Column(name = "country", length = 20, nullable = false)
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
