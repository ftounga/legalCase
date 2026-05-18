package fr.ailegalcase.stylelearning;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.workspace.Workspace;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * F-98 / SF-98-46 — document du corpus de style d'un cabinet.
 *
 * <p>Entité de <strong>niveau workspace</strong> (FK {@code workspace_id} porte
 * l'isolation multi-tenant) — modèle {@code UserBillingRate}. L'avocat téléverse
 * ses anciennes conclusions ; le worker asynchrone produit une
 * {@code styleSignature} (description réutilisable du style rédactionnel, sans
 * aucune donnée de dossier) puis purge le fichier source.</p>
 *
 * <p><strong>Minimisation RGPD</strong> (invariant 1 du cadrage) : aucun texte
 * brut n'est persisté — seule la {@code styleSignature} subsiste après traitement.
 * Elle est d'usage strictement interne (SF-98-47) et n'est jamais exposée par l'API.</p>
 */
@Entity
@Table(name = "style_corpus_documents")
@Getter
@Setter
public class StyleCorpusDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workspace_id", nullable = false)
    private Workspace workspace;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(name = "original_filename", nullable = false, length = 512)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 120)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StyleCorpusDocumentStatus status;

    /** Description du style rédactionnel produite par l'IA — usage interne SF-98-47, jamais exposée. */
    @Column(name = "style_signature", columnDefinition = "TEXT")
    private String styleSignature;

    /** Un document ingéré contribue au profil de style du cabinet par défaut. */
    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

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
