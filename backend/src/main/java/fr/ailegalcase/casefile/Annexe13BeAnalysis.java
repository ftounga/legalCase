package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-IM-08-05 : entity 1:1 par dossier portant l'analyse d'un ordre de quitter le territoire
 * belge (annexe 13 AR 08/10/1981 d'exécution de la Loi 15/12/1980). Outil single-country BE.
 */
@Entity
@Table(name = "annexe13_be_analyses")
@Getter
@Setter
public class Annexe13BeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_notification_annexe13", nullable = false)
    private LocalDate dateNotificationAnnexe13;

    @Column(name = "delai_depart_impose_jours", nullable = false)
    private int delaiDepartImposeJours;

    @Column(name = "motif_oqt", nullable = false, length = 40)
    private String motifOqt;

    @Column(name = "transfert_imminent", nullable = false)
    private boolean transfertImminent;

    @Column(name = "recours_forme", nullable = false)
    private boolean recoursForme;

    @Column(name = "date_recours")
    private LocalDate dateRecours;

    @Column(name = "type_recours", length = 30)
    private String typeRecours;

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
