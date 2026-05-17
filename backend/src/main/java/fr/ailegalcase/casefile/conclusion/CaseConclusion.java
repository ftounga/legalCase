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
 * <p>Relation 1:1 avec {@link CaseFile} ({@code case_file_id} UNIQUE) : régénérer
 * réutilise la ligne (UPDATE), ne crée jamais de doublon. Le {@code workspace_id}
 * porte l'isolation multi-tenant et est contrôlé à chaque accès.</p>
 *
 * <p>Les codes {@code jurisdictionCode}/{@code stageCode}/{@code positionCode} sont
 * un snapshot du stade procédural du dossier figé au déclenchement.</p>
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
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CaseConclusionStatus status;

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
