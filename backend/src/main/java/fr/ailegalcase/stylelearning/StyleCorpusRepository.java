package fr.ailegalcase.stylelearning;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * F-98 / SF-98-46 — accès aux documents du corpus de style.
 */
public interface StyleCorpusRepository extends JpaRepository<StyleCorpusDocument, UUID> {

    /** Documents du corpus d'un workspace, triés date de création décroissante. */
    List<StyleCorpusDocument> findByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

    /** Charge un document en exigeant son rattachement au workspace — isolation 404. */
    Optional<StyleCorpusDocument> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    /**
     * Signatures de style exploitables du cabinet — usage SF-98-47 (injection dans
     * la génération) : documents actifs dont l'extraction a abouti ({@code DONE}).
     */
    List<StyleCorpusDocument> findByWorkspaceIdAndActiveTrueAndStatus(
            UUID workspaceId, StyleCorpusDocumentStatus status);
}
