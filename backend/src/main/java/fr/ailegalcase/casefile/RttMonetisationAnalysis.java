package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * SF-218-37 : entity 1:1 par dossier portant l'analyse de monétisation de jours
 * de RTT (loi n° 2022-1157 du 16/08/2022 art. 5, F-DT-51). Outil single-country
 * FR.
 */
@Entity
@Table(name = "rtt_monetisation_analyses")
@Getter
@Setter
public class RttMonetisationAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "nombre_jours_rtt_renonces", nullable = false)
    private int nombreJoursRttRenonces;

    @Column(name = "salaire_journalier_brut", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireJournalierBrut;

    @Column(name = "taux_applique", nullable = false)
    private double tauxApplique;

    @Column(name = "jours_acquis_dans_fenetre", nullable = false)
    private boolean joursAcquisDansFenetre;

    @Column(name = "montant_brut", precision = 14, scale = 2)
    private BigDecimal montantBrut;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private RttMonetisationStatut statut;

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
