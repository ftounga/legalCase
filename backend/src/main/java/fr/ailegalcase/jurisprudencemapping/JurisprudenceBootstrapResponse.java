package fr.ailegalcase.jurisprudencemapping;

/**
 * F-JU-01 / SF-JU-01-05 — réponse bootstrap.
 */
public record JurisprudenceBootstrapResponse(
        int entriesProcessed,
        int mappingsCreated,
        int entriesSkipped,
        long durationMs) {
}
