package fr.ailegalcase.document;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "case_file_id", nullable = false)
    private CaseFile caseFile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    @Column(nullable = false, length = 1024)
    private String storageKey;

    @Column(nullable = false)
    private Instant createdAt;

    /**
     * SF-122-03 : si true, l'OCR Textract fera de l'analyse approfondie
     * (FeatureType.TABLES + FORMS) et le document comptera ×3 dans le quota OCR.
     * Défini à l'upload par l'avocat via la checkbox "Analyse approfondie".
     */
    @Column(name = "ocr_forms_mode", nullable = false)
    private boolean ocrFormsMode = false;

    /**
     * SF-122-07 : si true (défaut), un PDF scanné déclenche l'OCR Textract en
     * fallback de PDFBox vide. Si false, le scan reste FAILED EMPTY_TEXT sans
     * consommer de quota OCR — utile pour les docs d'archivage / partage
     * client qui n'ont pas besoin d'être dans l'analyse IA.
     * L'avocat peut toujours activer l'OCR a posteriori via le bouton retry.
     */
    @Column(name = "ocr_enabled", nullable = false)
    private boolean ocrEnabled = true;

    /**
     * SF-261-01 : si true, ce document est marqué « écritures adverses » par
     * l'avocat — il porte les conclusions de la partie adverse. Pré-requis de
     * l'extraction des moyens adverses (SF-261-02) puis de leur réfutation
     * (SF-261-03). Posé par jugement humain ; tout document naît non marqué.
     * Distinct du marquage d'une citation adverse (SF-98-56, jurisprudence_checks).
     */
    @Column(name = "adverse_pleadings", nullable = false)
    private boolean adversePleadings = false;

    @PrePersist
    void onPrePersist() {
        this.createdAt = Instant.now();
    }
}
