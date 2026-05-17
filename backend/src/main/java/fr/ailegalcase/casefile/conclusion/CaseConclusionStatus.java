package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 / SF-98-01 — statut persisté d'un projet de conclusions.
 *
 * <p>{@code NOT_GENERATED} n'est <strong>pas</strong> une valeur de cet enum : c'est une
 * valeur de réponse synthétique du DTO {@code ConclusionResponse}, renvoyée quand aucune
 * ligne {@code case_conclusions} n'existe pour le dossier.</p>
 */
public enum CaseConclusionStatus {

    /** Ligne créée, message RabbitMQ publié, worker pas encore démarré. */
    PENDING,

    /** Worker en cours d'assemblage du prompt / d'appel IA. */
    PROCESSING,

    /** Génération terminée — {@code content} renseigné. */
    DONE,

    /** Génération en échec — {@code errorMessage} renseigné, relance possible. */
    FAILED
}
