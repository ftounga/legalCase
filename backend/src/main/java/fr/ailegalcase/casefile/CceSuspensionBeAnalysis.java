package fr.ailegalcase.casefile;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-221-05 : entity 1:1 par dossier portant l'analyse du recours CCE en SUSPENSION
 * ORDINAIRE (référé administratif : art. 39/82 Loi 15/12/1980 ; loi 15/09/2006).
 * Outil <b>single-country BELGIQUE</b>.
 *
 * <p>Snapshot JSON complet (inputs + outputs) dans {@code result_data} pour permettre la
 * restitution UI sans recalcul (pattern F-DT-42).
 */
@Entity
@Table(name = "cce_suspension_be_analyses")
@Getter
@Setter
public class CceSuspensionBeAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @Column(name = "date_notification_decision", nullable = false)
    private LocalDate dateNotificationDecision;

    @Column(name = "recours_annulation_introduit", nullable = false)
    private Boolean recoursAnnulationIntroduit;

    @Column(name = "urgence_invocable", nullable = false)
    private Boolean urgenceInvocable;

    @Column(name = "risque_prejudice_grave", nullable = false)
    private Boolean risquePrejudiceGraveDifficilementReparable;

    @Column(name = "mesure_eloignement_imminente", nullable = false)
    private Boolean mesureEloignementImminente;

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
