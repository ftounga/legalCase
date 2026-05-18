package fr.ailegalcase.stylelearning;

/**
 * F-98 / SF-98-46 — statut persisté d'un document du corpus de style.
 *
 * <p>Cycle : {@code PENDING} (ligne créée, fichier S3 stocké, message RabbitMQ publié)
 * → {@code PROCESSING} (worker en cours d'extraction / d'appel IA) →
 * {@code DONE} ({@code style_signature} renseignée, fichier S3 purgé) ou
 * {@code FAILED} ({@code error_message} renseigné, fichier S3 purgé tout de même).</p>
 */
public enum StyleCorpusDocumentStatus {

    /** Ligne créée, fichier S3 stocké temporairement, message RabbitMQ publié. */
    PENDING,

    /** Worker en cours d'extraction de texte / d'appel IA. */
    PROCESSING,

    /** Traitement terminé — {@code style_signature} renseignée, fichier S3 purgé. */
    DONE,

    /** Traitement en échec — {@code error_message} renseigné, fichier S3 purgé tout de même. */
    FAILED
}
