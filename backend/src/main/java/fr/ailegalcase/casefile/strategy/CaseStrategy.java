package fr.ailegalcase.casefile.strategy;

import fr.ailegalcase.casefile.CaseFile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-286 / SF-286-01 — recommandation stratégique consolidée d'un dossier.
 *
 * <p>Couche de <b>LECTURE</b> par-dessus les outils décisionnels : une ligne courante
 * par dossier (upsert), portant la reco générée par un appel LLM dédié à partir des
 * <b>verdicts des outils CALCULÉS</b> (jamais des tuiles pré-remplies non cliquées) et
 * de la <b>synthèse</b>. Elle n'altère, ne réordonne, ne modifie <b>AUCUN</b> outil
 * décisionnel ni sa visibilité — invariant « 1 outil = 1 situation » préservé.</p>
 *
 * <p>Isolation workspace via {@link CaseFile}.</p>
 */
@Entity
@Table(name = "case_strategy")
@Getter
@Setter
public class CaseStrategy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false, unique = true)
    private CaseFile caseFile;

    /** Markdown de la recommandation stratégique (null si génération sans entrée exploitable). */
    @Column(columnDefinition = "text")
    private String content;

    /** Nombre de verdicts d'outils CALCULÉS pris en compte lors de la génération. */
    @Column(name = "tools_considered", nullable = false)
    private int toolsConsidered;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "generated_at", nullable = false)
    private Instant generatedAt;

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
