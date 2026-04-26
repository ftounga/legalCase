package fr.ailegalcase.casefile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-24-07 : entity 1:1 par dossier portant l'analyse de réserve
 * héréditaire et action en réduction (FR — art. 913 + 914-1 + 920-928 Cciv).
 */
@Entity
@Table(name = "reserve_heriditaire_analyses")
@Getter
@Setter
public class ReserveHereditaireAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "nombre_enfants", nullable = false)
    private int nombreEnfants;

    @Column(name = "conjoint_survivant", nullable = false)
    private boolean conjointSurvivant;

    @Column(name = "montant_succession", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantSuccession;

    @Column(name = "montant_libs_total", nullable = false, precision = 15, scale = 2)
    private BigDecimal montantLibsTotal;

    @Column(name = "date_ouverture_succession", nullable = false)
    private LocalDate dateOuvertureSuccession;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite_demandeur", nullable = false, length = 48)
    private ReserveHereditaireCalculator.QualiteDemandeur qualiteDuDemandeur;

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
