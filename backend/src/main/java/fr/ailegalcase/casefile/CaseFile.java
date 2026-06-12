package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_files")
@Getter
@Setter
public class CaseFile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 50)
    private String legalDomain;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Column
    private Instant lastDocumentDeletedAt;

    @Column
    private Instant deletedAt;

    /** F-243 : stade procédural — code juridiction du référentiel {@code ProcedureStageCatalog}. Nullable. */
    @Column(name = "procedure_jurisdiction", length = 50)
    private String procedureJurisdiction;

    /** F-243 : stade procédural — code stade rattaché à la juridiction. Nullable. */
    @Column(name = "procedure_stage", length = 50)
    private String procedureStage;

    /** F-243 : stade procédural — code position juridique valide pour le stade. Nullable. */
    @Column(name = "procedure_position", length = 50)
    private String procedurePosition;

    // ── F-285 : qualification d'entrée du dossier (intake guidé). Tous nullable. ──

    /** Type de litige (texte libre court, ex. « Contestation de licenciement »). */
    @Column(name = "intake_dispute_type", length = 120)
    private String intakeDisputeType;

    /** Verdict de recevabilité / compétence. */
    @Enumerated(EnumType.STRING)
    @Column(name = "intake_admissibility", length = 20)
    private AdmissibilityStatus intakeAdmissibility;

    /** Statut de prescription / forclusion. */
    @Enumerated(EnumType.STRING)
    @Column(name = "intake_prescription_status", length = 24)
    private PrescriptionStatus intakePrescriptionStatus;

    /** Date limite de prescription / forclusion (optionnelle). */
    @Column(name = "intake_prescription_deadline")
    private java.time.LocalDate intakePrescriptionDeadline;

    /** Juridiction compétente (texte libre court, ex. « Conseil de prud'hommes »). */
    @Column(name = "intake_jurisdiction", length = 120)
    private String intakeJurisdiction;

    /** Valeur estimée du litige, en centimes d'EUR (optionnelle, ≥ 0). */
    @Column(name = "intake_estimated_value_cents")
    private Long intakeEstimatedValueCents;

    /** Note de qualification libre (optionnelle). */
    @Column(name = "intake_notes", length = 1000)
    private String intakeNotes;

    /** Horodatage de la 1ʳᵉ qualification (null tant que jamais qualifié, préservé ensuite). */
    @Column(name = "intake_qualified_at")
    private Instant intakeQualifiedAt;

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
