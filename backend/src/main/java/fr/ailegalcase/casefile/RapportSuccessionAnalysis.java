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
 * SF-FA-24-13 : entity 1:1 par dossier portant l'analyse de rapport à
 * succession (FR — art. 843-863 + 919 Cciv).
 */
@Entity
@Table(name = "rapport_succession_analyses")
@Getter
@Setter
public class RapportSuccessionAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "donations_recues_eur", nullable = false, precision = 15, scale = 2)
    private BigDecimal donationsRecuesEur;

    @Column(name = "date_donation", nullable = false)
    private LocalDate dateDonation;

    @Column(name = "valeur_au_jour_partage", nullable = false, precision = 15, scale = 2)
    private BigDecimal valeurAuJourPartage;

    @Column(name = "donation_dispense_de_rapport", nullable = false)
    private boolean donationDispenseDeRapport;

    @Column(name = "nature_presumee_non_rapportable", nullable = false)
    private boolean naturePresumeeNonRapportable;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite_heritier", nullable = false, length = 48)
    private RapportSuccessionCalculator.QualiteHeritier qualiteHeritier;

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
