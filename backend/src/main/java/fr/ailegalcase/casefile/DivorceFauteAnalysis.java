package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-09-01 : entity 1:1 par dossier portant l'analyse divorce pour faute FR
 * (art. 242-246, 266, 270 Cciv). Outil single-country FR.
 */
@Entity
@Table(name = "divorce_faute_analyses")
@Getter
@Setter
public class DivorceFauteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    /** JSON array (stringifié) des codes de fautes invoquées. */
    @Column(name = "fautes_invoquees", nullable = false, columnDefinition = "TEXT")
    private String fautesInvoquees = "[]";

    @Column(name = "preuves_documentaires", nullable = false)
    private boolean preuvesDocumentaires;

    @Column(name = "torts_adverse_invoques", nullable = false)
    private boolean tortsAdverseInvoques;

    @Column(name = "duree_mariage_annees", nullable = false)
    private int dureeMariageAnnees;

    @Column(name = "revenus_annuels_demandeur_eur", precision = 15, scale = 2)
    private BigDecimal revenusAnnuelsDemandeurEur;

    @Column(name = "revenus_annuels_defendeur_eur", precision = 15, scale = 2)
    private BigDecimal revenusAnnuelsDefendeurEur;

    @Column(name = "date_depot_assignation")
    private LocalDate dateDepotAssignation;

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
