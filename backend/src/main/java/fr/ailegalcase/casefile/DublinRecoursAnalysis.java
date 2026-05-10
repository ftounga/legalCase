package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-208-02 : entity 1:1 par dossier portant l'analyse du recours Dublin
 * (CESEDA L.572+ + Reg. UE 604/2013). Outil single-country FR.
 */
@Entity
@Table(name = "dublin_recours_analyses")
@Getter
@Setter
public class DublinRecoursAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_notification_decision_transfert", nullable = false)
    private LocalDate dateNotificationDecisionTransfert;

    @Column(name = "etat_membre_responsable", length = 60)
    private String etatMembreResponsable;

    @Column(name = "motif_transfert", nullable = false, length = 40)
    private String motifTransfert;

    @Column(name = "recours_forme", nullable = false)
    private boolean recoursForme;

    @Column(name = "date_recours")
    private LocalDate dateRecours;

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
