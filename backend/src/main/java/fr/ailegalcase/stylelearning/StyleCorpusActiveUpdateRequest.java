package fr.ailegalcase.stylelearning;

/**
 * F-98 / SF-98-46 — corps du {@code PATCH} d'activation / désactivation d'un
 * document de corpus.
 *
 * @param active {@code true} pour réactiver, {@code false} pour désactiver
 */
public record StyleCorpusActiveUpdateRequest(Boolean active) {
}
