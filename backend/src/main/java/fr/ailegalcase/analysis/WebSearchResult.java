package fr.ailegalcase.analysis;

/**
 * F-179 SF-179-02 — résultat d'une recherche web de vérification d'existence
 * d'une référence jurisprudentielle.
 *
 * @param outcome   issue de la recherche
 * @param sourceUrl URL de l'arrêt en ligne (renseignée uniquement si
 *                  {@code outcome == FOUND}), sinon {@code null}
 */
public record WebSearchResult(Outcome outcome, String sourceUrl) {

    /** Issue d'une recherche web. */
    public enum Outcome {
        /** L'arrêt a été trouvé en ligne — existence confirmée. */
        FOUND,
        /** L'arrêt est confirmé introuvable (résultat vide non ambigu). */
        NOT_FOUND,
        /** Recherche non concluante (timeout, erreur HTTP, résultat ambigu). */
        UNCERTAIN
    }

    static WebSearchResult found(String url) {
        return new WebSearchResult(Outcome.FOUND, url);
    }

    static WebSearchResult notFound() {
        return new WebSearchResult(Outcome.NOT_FOUND, null);
    }

    static WebSearchResult uncertain() {
        return new WebSearchResult(Outcome.UNCERTAIN, null);
    }
}
