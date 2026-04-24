package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * SF-IM-08-03 : entity 1:1 par dossier portant l'analyse d'une OQTF <b>sans</b> délai de départ
 * volontaire (art. L.731-1 CESEDA). Procédure 48h devant juge unique TA (L.614-8).
 * Outil single-country FR.
 */
@Entity
@Table(name = "oqtf_sans_delai_analyses")
@Getter
@Setter
public class OqtfSansDelaiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_heure_notification_oqtf", nullable = false)
    private LocalDateTime dateHeureNotificationOqtf;

    @Column(name = "motif_sans_delai", nullable = false, length = 40)
    private String motifSansDelai;

    @Column(name = "placement_cra", nullable = false)
    private boolean placementCra;

    @Column(name = "recours_forme", nullable = false)
    private boolean recoursForme;

    @Column(name = "date_heure_recours")
    private LocalDateTime dateHeureRecours;

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
