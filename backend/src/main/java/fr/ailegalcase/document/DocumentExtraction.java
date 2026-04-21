package fr.ailegalcase.document;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_extractions")
@Getter
@Setter
public class DocumentExtraction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Enumerated(EnumType.STRING)
    @Column(name = "extraction_status", nullable = false, length = 20)
    private ExtractionStatus extractionStatus;

    /**
     * SF-121-01 : motif qualifiant l'échec. Renseigné uniquement si
     * {@code extractionStatus == FAILED}, null sinon.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "failure_reason", length = 50)
    private ExtractionFailureReason failureReason;

    /**
     * SF-144-01 : flag intermédiaire "OCR Textract en cours". Commité dans une
     * transaction séparée juste avant OcrService.tryOcr, remis à false après.
     * Lu par le polling frontend pour afficher un feedback contextuel.
     */
    @Column(name = "ocr_running", nullable = false)
    private boolean ocrRunning = false;

    @Column(name = "extracted_text", length = Integer.MAX_VALUE)
    private String extractedText;

    @Column(name = "extraction_metadata", length = Integer.MAX_VALUE)
    private String extractionMetadata;

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
