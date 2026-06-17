package fr.ailegalcase.analysis;

import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-194 SF-194-01 — Statut avocat trichotomique (`A_DEMANDER` / `OBTENUE` /
 * `NON_APPLICABLE`) appliqué à une pièce manquante extraite par l'IA dans
 * {@code case_analyses.analysis_result.pieces_manquantes}.
 *
 * <p>Overlay sur le JSON pieces_manquantes existant (Option A retenue 2026-05-06) :
 * la source de vérité IA reste le tableau JSON, ce statut s'y joint au runtime
 * via le libellé normalisé {@code (case_file_id, piece_libelle_normalise)} —
 * pour préserver F-92 et F-IA-03 strictement.</p>
 *
 * <p>Le statut par défaut implicite (sans entrée table) = {@code A_DEMANDER}.</p>
 *
 * <p>Pattern miroir {@link StrategicOption} (F-176) et {@link ProcedureCheck}
 * (F-96), mais portant directement sur la clé texte (pas un identifiant
 * stable comme {@code piste_id} ou {@code check_id}, car le JSON
 * pieces_manquantes n'a pas de PRIMARY KEY individuel par pièce).</p>
 */
@Entity
@Table(name = "piece_manquante_status",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_pms_case_file_piece_norm",
                columnNames = {"case_file_id", "piece_libelle_normalise"}))
@Getter
@Setter
public class PieceManquanteStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @Column(name = "piece_libelle_normalise", nullable = false, length = 500)
    private String pieceLibelleNormalise;

    @Column(name = "piece_libelle_original", nullable = false, length = 500)
    private String pieceLibelleOriginal;

    @Column(name = "statut", nullable = false, length = 20)
    private String statut;

    @Column(name = "raison_non_app", columnDefinition = "TEXT")
    private String raisonNonApp;

    @Column(name = "destinataire", length = 200)
    private String destinataire;

    /**
     * SF-194-04 — document du dossier lié à la pièce reçue (statut OBTENUE), pour
     * la traçabilité fait→pièce. Nullable : une pièce peut être marquée obtenue
     * sans lier de fichier (rétro-compat). Référence souple (pas de FK stricte —
     * le document peut être supprimé ; lien orphelin toléré).
     */
    @Column(name = "obtained_document_id")
    private UUID obtainedDocumentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Statuts trichotomiques cohérents F-176 (équivalent TO_STUDY / RETAINED / DISCARDED). */
    public static final String STATUT_A_DEMANDER = "A_DEMANDER";
    public static final String STATUT_OBTENUE = "OBTENUE";
    public static final String STATUT_NON_APPLICABLE = "NON_APPLICABLE";

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
