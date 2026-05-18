package fr.ailegalcase.casefile.jurisprudence;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-242 / SF-242-01 — une citation de jurisprudence d'appui saisie manuellement par
 * l'avocat, rattachée à un point juridique de la synthèse d'un dossier.
 *
 * <p>Option δ (citation structurée) : on persiste la référence ({@code reference}) et
 * une ligne de portée ({@code portee}), pas le texte intégral de l'arrêt. Le point
 * juridique est identifié par son index dans la synthèse ({@code pointJuridiqueIndex})
 * doublé d'un snapshot de son texte ({@code pointJuridiqueTexte}) — le snapshot permet
 * l'affichage même après une ré-analyse qui décalerait les index.</p>
 *
 * <p>Isolée par {@code workspace_id}, comme {@code case_files} / {@code case_conclusions}.
 * Toute mutation ({@code updatedAt}) participe au calcul de péremption des conclusions
 * (SF-98-53).</p>
 */
@Entity
@Table(name = "case_jurisprudence_citations")
@Getter
@Setter
public class JurisprudenceCitation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    /** Index du point juridique dans la synthèse du dossier (≥ 0). */
    @Column(name = "point_juridique_index", nullable = false)
    private int pointJuridiqueIndex;

    /** Snapshot du texte du point juridique au moment de la saisie. */
    @Column(name = "point_juridique_texte", nullable = false, length = 2000)
    private String pointJuridiqueTexte;

    /** Libellé de la référence (ex. "Cass. soc. 12 oct. 2022, n° 21-12345"). */
    @Column(nullable = false, length = 255)
    private String reference;

    /** Ligne de portée de la référence (optionnelle). */
    @Column(length = 500)
    private String portee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onPrePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void onPreUpdate() {
        this.updatedAt = Instant.now();
    }
}
