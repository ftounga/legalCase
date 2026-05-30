package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-218-05 : entity 1:1 par dossier portant l'analyse d'un pourvoi en
 * cassation devant la chambre sociale (art. 612 CPC ; art. 604 CPC ; art. 973
 * CPC ; art. 1014 CPC). Outil single-country FR.
 */
@Entity
@Table(name = "pourvoi_cassation_soc_analyses")
@Getter
@Setter
public class PourvoiCassationSocAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_notification_arret", nullable = false)
    private LocalDate dateNotificationArret;

    @Column(name = "date_limite_pourvoi", nullable = false)
    private LocalDate dateLimitePourvoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict_delai", nullable = false, length = 20)
    private PourvoiCassationSocVerdictDelai verdictDelai;

    @Enumerated(EnumType.STRING)
    @Column(name = "risque_non_admission", nullable = false, length = 10)
    private PourvoiCassationSocRisqueNonAdmission risqueNonAdmission;

    @Column(name = "representation_avocat_cassation", nullable = false)
    private boolean representationAvocatCassation;

    @Column(name = "moyen_serieux_identifie", nullable = false)
    private boolean moyenSerieuxIdentifie;

    @Enumerated(EnumType.STRING)
    @Column(name = "verdict", nullable = false, length = 30)
    private PourvoiCassationSocVerdict verdict;

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
