package fr.ailegalcase.stylelearning;

import java.util.UUID;

/**
 * F-98 / SF-98-46 — réponse {@code 202} du téléversement d'un document de corpus.
 *
 * <p>Contrat figé : {@code {"id":UUID,"status":"PENDING"}}.</p>
 */
public record StyleCorpusUploadResponse(UUID id, StyleCorpusDocumentStatus status) {

    public static StyleCorpusUploadResponse pending(UUID id) {
        return new StyleCorpusUploadResponse(id, StyleCorpusDocumentStatus.PENDING);
    }
}
