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
 * SF-FA-18-05 : entity 1:1 par dossier portant l'analyse de recevabilité
 * d'une action en recherche de paternité (FR — art. 327 + 340 + 16-11 + 321 Cciv).
 */
@Entity
@Table(name = "recherche_paternite_analyses")
@Getter
@Setter
public class RecherchePaterniteAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualite_du_demandeur", nullable = false, length = 50)
    private RecherchePaterniteCalculator.QualiteDuDemandeur qualiteDuDemandeur;

    @Column(name = "date_naissance_enfant", nullable = false)
    private LocalDate dateNaissanceEnfant;

    @Column(name = "presomption_possession_etat", nullable = false)
    private boolean presomptionPossessionEtat;

    @Column(name = "expertise_adn_demandee", nullable = false)
    private boolean expertiseAdnDemandee;

    @Column(name = "pere_designe_refuse_adn", nullable = false)
    private boolean pereDesigneRefuseADN;

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
