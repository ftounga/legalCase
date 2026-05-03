package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "case_analyses")
@Getter
@Setter
public class CaseAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @Column(name = "version", nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_type", nullable = false, length = 20)
    private AnalysisType analysisType;

    @Enumerated(EnumType.STRING)
    @Column(name = "analysis_status", nullable = false, length = 20)
    private AnalysisStatus analysisStatus;

    @Column(name = "analysis_result", length = Integer.MAX_VALUE)
    private String analysisResult;

    /**
     * F-185 SF-185-01 — état partiel persisté pendant le streaming Sonnet.
     * Accumulé section JSON par section JSON ; purgé (NULL) au DONE/FAILED quand
     * {@code analysisResult} complet le remplace. Permet le refresh-safe sur la
     * page synthèse pendant le streaming.
     * Stocké en JSONB côté DB (cf. migration 201).
     */
    @Column(name = "partial_state", columnDefinition = "TEXT")
    private String partialState;

    /**
     * F-185 SF-185-03 — analyse synthétique générée automatiquement au fil des
     * DocumentAnalysis DONE (vs déclenchement manuel par l'avocat).
     * Les analyses provisoires masquent leur verdict (riskLevel grisé + bandeau
     * "Synthèse provisoire — N/M documents") jusqu'à ce qu'une analyse
     * définitive (manuelle ou auto-finale) la remplace.
     */
    @Column(name = "is_provisional", nullable = false)
    private boolean isProvisional = false;

    @Column(name = "model_used", length = 100)
    private String modelUsed;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "faits_count")
    private Integer faitsCount;

    @Column(name = "points_juridiques_count")
    private Integer pointsJuridiquesCount;

    @Column(name = "risques_count")
    private Integer risquesCount;

    @Column(name = "questions_ouvertes_count")
    private Integer questionsOuvertesCount;

    @Column(name = "timeline_count")
    private Integer timelineCount;

    @Column(name = "risk_level", length = 10)
    private String riskLevel;

    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
