package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-179 SF-179-01 — une vérification de référence jurisprudentielle détectée
 * dans un document uploadé d'un dossier.
 *
 * <p>Rattachée à la {@link CaseAnalysis} qui l'a produite ; isolée par
 * {@code workspace_id}. Les colonnes {@code source_url} / {@code web_search_used}
 * sont renseignées par le fallback web search (SF-179-02).</p>
 */
@Entity
@Table(name = "jurisprudence_checks")
@Getter
@Setter
public class JurisprudenceCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_analysis_id", nullable = false)
    private CaseAnalysis caseAnalysis;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /** Nom du document dans lequel la référence a été détectée. */
    @Column(name = "document_name", nullable = false, length = 500)
    private String documentName;

    /** Libellé canonique de la référence (ex. "Cass. soc. 25 sept. 2013, n° 12-17.516"). */
    @Column(nullable = false, length = 500)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private JurisprudenceCheckStatus statut = JurisprudenceCheckStatus.UNCERTAIN;

    /** Explication courte du verdict (réalité de l'arrêt). */
    @Column(columnDefinition = "TEXT")
    private String explication;

    /** Position juridique que le document prête à l'arrêt (pour les SUSPECT). */
    @Column(name = "position_alleguee", columnDefinition = "TEXT")
    private String positionAlleguee;

    /** Lien source (Légifrance / Juridat) — renseigné par SF-179-02. */
    @Column(name = "source_url", length = 1000)
    private String sourceUrl;

    /** Confiance Claude : HIGH / MEDIUM / LOW. */
    @Column(name = "claude_confidence", length = 10)
    private String claudeConfidence;

    /** true si le fallback web search a été tenté pour cette référence (SF-179-02). */
    @Column(name = "web_search_used", nullable = false)
    private boolean webSearchUsed = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onPrePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
