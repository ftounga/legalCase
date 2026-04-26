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

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-FA-24-09 : entity 1:1 par dossier portant l'analyse de la modalité de
 * partage successoral (FR — art. 815-840 Cciv + 1364 CPC).
 */
@Entity
@Table(name = "partage_successoral_analyses")
@Getter
@Setter
public class PartageSuccessoralAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_partage_demande", nullable = false, length = 30)
    private PartageSuccessoralCalculator.ModePartage modePartageDemande;

    @Column(name = "nombre_coheritiers", nullable = false)
    private int nombreCoheritiers;

    @Column(name = "consentements_tous", nullable = false)
    private boolean consentementsTous;

    @Column(name = "presence_immeubles", nullable = false)
    private boolean presenceImmeubles;

    @Column(name = "accords_valuation", nullable = false)
    private boolean accordsValuation;

    @Column(name = "desaccord_persistant", nullable = false)
    private boolean desaccordPersistant;

    @Column(name = "date_deces", nullable = false)
    private LocalDate dateDeces;

    @Column(name = "valeur_masse_eur", nullable = false)
    private double valeurMasseEur;

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
