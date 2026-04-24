package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-10-01 : entity 1:1 par dossier portant l'analyse "divorce accepté" /
 * acceptation du principe de la rupture (art. 233-234 Cciv + art. 1123 CPC).
 * Outil single-country FR + DROIT_FAMILLE.
 */
@Entity
@Table(name = "divorce_accepte_analyses")
@Getter
@Setter
public class DivorceAccepteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "acceptation_principe_signee", nullable = false)
    private boolean acceptationPrincipeSignee;

    @Column(name = "date_acceptation_pv")
    private LocalDate dateAcceptationPV;

    @Column(name = "duree_mariage_annees", nullable = false)
    private int dureeMariageAnnees;

    @Column(name = "revenus_annuels_epoux1_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal revenusAnnuelsEpoux1Eur = BigDecimal.ZERO;

    @Column(name = "revenus_annuels_epoux2_eur", nullable = false, precision = 12, scale = 2)
    private BigDecimal revenusAnnuelsEpoux2Eur = BigDecimal.ZERO;

    @Column(name = "patrimoine_commun", nullable = false)
    private boolean patrimoineCommun;

    @Column(name = "date_assignation")
    private LocalDate dateAssignation;

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
