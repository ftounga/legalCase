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
 * SF-FA-18-03 : entity 1:1 par dossier portant l'analyse de recevabilité d'une
 * action en contestation de paternité (FR — art. 332-335 Cciv).
 */
@Entity
@Table(name = "contestation_paternite_analyses")
@Getter
@Setter
public class ContestationPaterniteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite_a_agir", nullable = false, length = 50)
    private ContestationPaterniteCalculator.QualiteAagir qualiteAagir;

    @Column(name = "date_etablissement_filiation", nullable = false)
    private LocalDate dateEtablissementFiliation;

    @Column(name = "date_connaissance_verite", nullable = false)
    private LocalDate dateConnaissanceVerite;

    @Column(name = "date_majorite_enfant")
    private LocalDate dateMajoriteEnfant;

    @Column(name = "possession_etat_conforme_5ans", nullable = false)
    private boolean possessionEtatConforme5Ans;

    @Column(name = "expertise_adn_demandee", nullable = false)
    private boolean expertiseAdnDemandee;

    @Column(name = "motifs_serieux", nullable = false)
    private boolean motifsSerieux;

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
