package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-98 / SF-98-01 — projet de conclusions généré par IA pour un dossier.
 *
 * <p>SF-98-52 : relation <strong>1:N</strong> avec {@link CaseFile} — chaque
 * génération produit une nouvelle version ({@code version_number} incrémental,
 * unicité {@code (case_file_id, version_number)}). Le {@code workspace_id} porte
 * l'isolation multi-tenant et est contrôlé à chaque accès.</p>
 *
 * <p>Les codes {@code jurisdictionCode}/{@code stageCode}/{@code positionCode} sont
 * un snapshot du stade procédural du dossier figé au déclenchement.</p>
 *
 * <p>{@code lifecycleStatus} ({@code DRAFT}/{@code VALIDATED}/{@code DEPOSITED})
 * porte le cycle de vie de la version, distinct du {@code status} de génération IA.</p>
 */
@Entity
@Table(name = "case_conclusions")
@Getter
@Setter
public class CaseConclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /** Numéro de version, incrémental par dossier (unicité {@code (case_file_id, version_number)}). */
    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaseConclusionStatus status;

    /** Cycle de vie de la version — brouillon / validé / déposé. */
    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 20)
    private ConclusionLifecycleStatus lifecycleStatus;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "jurisdiction_code", nullable = false, length = 50)
    private String jurisdictionCode;

    @Column(name = "stage_code", nullable = false, length = 50)
    private String stageCode;

    @Column(name = "position_code", nullable = false, length = 50)
    private String positionCode;

    @Column(name = "model_used", length = 80)
    private String modelUsed;

    /**
     * F-98 / SF-98-47 — vrai si la génération a adopté le style rédactionnel appris
     * du cabinet (au moins une signature de style active utilisée). Défaut {@code false}
     * (génération générique, comportement SF-98-01).
     */
    @Column(name = "style_applied", nullable = false)
    private boolean styleApplied;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
