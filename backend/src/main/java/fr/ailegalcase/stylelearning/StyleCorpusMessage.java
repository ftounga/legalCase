package fr.ailegalcase.stylelearning;

import java.util.UUID;

/**
 * F-98 / SF-98-46 — message RabbitMQ déclenchant l'extraction asynchrone de la
 * signature de style d'un document de corpus.
 *
 * @param styleCorpusDocumentId identifiant de la ligne {@code style_corpus_documents} à traiter
 * @param storageKey            clé S3 du fichier source temporaire (purgé après extraction)
 */
public record StyleCorpusMessage(UUID styleCorpusDocumentId, String storageKey) {
}
