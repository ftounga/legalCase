package fr.ailegalcase.stylelearning;

import java.time.Instant;
import java.util.UUID;

/**
 * F-98 / SF-98-46 — DTO de restitution d'un document du corpus de style.
 *
 * <p>La {@code styleSignature} de l'entité <strong>n'est jamais exposée</strong>
 * (usage strictement interne SF-98-47) : ce DTO ne la porte pas.</p>
 */
public record StyleCorpusDocumentSummary(
        UUID id,
        String originalFilename,
        StyleCorpusDocumentStatus status,
        boolean active,
        Instant createdAt,
        String errorMessage
) {

    public static StyleCorpusDocumentSummary from(StyleCorpusDocument document) {
        return new StyleCorpusDocumentSummary(
                document.getId(),
                document.getOriginalFilename(),
                document.getStatus(),
                document.isActive(),
                document.getCreatedAt(),
                document.getErrorMessage());
    }
}
