package fr.ailegalcase.document;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * SF-145-01 : pièce juridique identifiée à l'intérieur d'un document uploadé.
 * Persistée après extraction DONE via {@link DocumentPieceDetectionService}.
 */
@Entity
@Table(name = "document_pieces")
@Getter
@Setter
public class DocumentPiece {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private DocumentPieceType type;

    @Column(name = "label", length = 500)
    private String label;

    @Column(name = "page_start", nullable = false)
    private int pageStart;

    @Column(name = "page_end", nullable = false)
    private int pageEnd;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    /**
     * F-260 / SF-260-01 : numéro de pièce <b>persistant</b> et stable au sein du
     * dossier (« Pièce n° X »). Attribué à la création = max(piece_number du
     * dossier) + 1 ; jamais réutilisé après suppression (trou conservé). Seul un
     * réordonnancement explicite (PUT …/pieces/order) renumérote 1..N. Source
     * unique lue par les conclusions (F-98) et les fiches (prud'homale /
     * tribunal du travail). Nullable pour les pièces antérieures à F-260 non
     * encore backfillées (la migration 598 backfille l'existant).
     */
    @Column(name = "piece_number")
    private Integer pieceNumber;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** SF-148-01 : description visuelle Claude Vision (null si non enrichi). */
    @Column(name = "visual_description", columnDefinition = "text")
    private String visualDescription;

    /** SF-148-01 : horodatage de l'enrichissement vision. */
    @Column(name = "vision_enriched_at")
    private Instant visionEnrichedAt;

    /** SF-148-01 : identifiant du modèle vision utilisé (ex: claude-haiku-4-5-20251001). */
    @Column(name = "vision_model", length = 80)
    private String visionModel;

    /** SF-148-03 : état de l'enrichissement visuel (pour spinner frontend). */
    @Enumerated(EnumType.STRING)
    @Column(name = "vision_status", nullable = false, length = 20)
    private VisionStatus visionStatus = VisionStatus.NOT_APPLICABLE;

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
