package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-DT-27-01 : entity 1:1 par dossier portant l'analyse de validité procédurale du licenciement
 * pour motif grave en Belgique (art. 35 Loi 03/07/1978).
 */
@Entity
@Table(name = "motif_grave_be_analyses")
@Getter
@Setter
public class MotifGraveBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_connaissance_fait", nullable = false)
    private LocalDate dateConnaissanceFait;

    @Column(name = "date_notification_rupture", nullable = false)
    private LocalDate dateNotificationRupture;

    @Column(name = "date_notification_motifs", nullable = false)
    private LocalDate dateNotificationMotifs;

    @Column(name = "anciennette_annees", nullable = false)
    private int anciennetteAnnees;

    @Column(name = "salaire_mensuel_reference", nullable = false, precision = 12, scale = 2)
    private BigDecimal salaireMensuelReference;

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
