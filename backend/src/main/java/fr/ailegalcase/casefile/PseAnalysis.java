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
 * SF-DT-14-01 : entity 1:1 par dossier portant l'analyse de validité d'un PSE (FR).
 *
 * <p>La liste {@code contenuMesures} est sérialisée dans {@code result_data} (JSON complet)
 * et dans la colonne dédiée {@code contenu_mesures_json} pour faciliter d'éventuels
 * filtres SQL.</p>
 */
@Entity
@Table(name = "pse_analyses")
@Getter
@Setter
public class PseAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "taille_entreprise_salaries", nullable = false)
    private int tailleEntrepriseSalaries;

    @Column(name = "nombre_licenciements_envisages", nullable = false)
    private int nombreLicenciementsEnvisages;

    @Column(name = "periode_jours", nullable = false)
    private int periodeJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_adoption", nullable = false, length = 40)
    private PseCalculator.ModeAdoption modeAdoption;

    @Enumerated(EnumType.STRING)
    @Column(name = "csae_consulte_avis", nullable = false, length = 20)
    private PseCalculator.AvisCse csaeConsulteAvis;

    @Enumerated(EnumType.STRING)
    @Column(name = "dreets_statut", nullable = false, length = 20)
    private PseCalculator.StatutDreets dreetsStatut;

    @Column(name = "date_notification_dreets")
    private LocalDate dateNotificationDreets;

    @Column(name = "contenu_mesures_json", nullable = false, columnDefinition = "TEXT")
    private String contenuMesuresJson = "[]";

    @Column(name = "date_projet", nullable = false)
    private LocalDate dateProjet;

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
