package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-35 : entity 1:1 par dossier portant l'analyse de validité d'une mesure
 * d'assignation à résidence (art. L. 731-1 CESEDA). Outil single-country FR.
 */
@Entity
@Table(name = "assignation_residence_analyses")
@Getter
@Setter
public class AssignationResidenceAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_notification_assignation", nullable = false)
    private LocalDate dateNotificationAssignation;

    @Column(name = "duree_assignation_jours", nullable = false)
    private int dureeAssignationJours;

    @Enumerated(EnumType.STRING)
    @Column(name = "motif_assignation", nullable = false, length = 40)
    private AssignationResidenceMotifEnum motifAssignation;

    @Column(name = "obligations_presentation", columnDefinition = "TEXT")
    private String obligationsPresentation;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    private AssignationResidenceStatut statut;

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
