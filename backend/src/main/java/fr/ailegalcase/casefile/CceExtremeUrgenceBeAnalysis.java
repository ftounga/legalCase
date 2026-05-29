package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-215-15 : entity 1:1 par dossier portant l'analyse du délai de recours en
 * <b>extrême urgence</b> devant le Conseil du Contentieux des Étrangers (CCE) —
 * 5 jours <b>ouvrables</b> (art. 39/82 §4 al. 2-3 Loi 15/12/1980). Outil
 * <b>single-country BELGIQUE</b>.
 *
 * <p>Pattern miroir de {@link CceAnnulationBeAnalysis} (SF-215-13) : snapshot
 * JSON complet (inputs + outputs) dans {@code result_data} pour permettre la
 * restitution UI sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "cce_extreme_urgence_be_analyses")
@Getter
@Setter
public class CceExtremeUrgenceBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_acte_executoire", nullable = false)
    private LocalDate dateActeExecutoire;

    @Column(name = "type_acte", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private CceExtremeUrgenceBeTypeActeEnum typeActe;

    @Column(name = "recours_forme", nullable = false)
    private Boolean recoursForme;

    @Column(name = "date_recours")
    private LocalDate dateRecours;

    @Column(name = "statut", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private CceExtremeUrgenceBeStatut statut;

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
