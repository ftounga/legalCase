package fr.ailegalcase.analysis;

import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "strategic_options")
@Getter
@Setter
public class StrategicOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_analysis_id", nullable = false)
    private CaseAnalysis caseAnalysis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(nullable = false)
    private int ordre;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String texte;

    @Column(name = "base_juridique", length = 500)
    private String baseJuridique;

    @Column(name = "horizon_temporel", length = 255)
    private String horizonTemporel;

    /** JSON-encoded array of strings (free-form conditions). H2- and PostgreSQL-compatible TEXT. */
    @Column(name = "conditions_json", columnDefinition = "TEXT")
    private String conditionsJson;

    @Column(length = 500)
    private String source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StrategicOptionStatus statut = StrategicOptionStatus.TO_STUDY;

    @Column(name = "raison_discard", columnDefinition = "TEXT")
    private String raisonDiscard;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
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
