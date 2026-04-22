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
