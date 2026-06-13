package fr.ailegalcase.casefile.conclusion;

/**
 * SF-287-01 — charge utile de l'événement SSE {@code progress} du streaming de
 * génération des conclusions.
 *
 * <p>Sérialisée en JSON et publiée à chaque fragment (throttlé) vers les emitters
 * du dossier ouverts sur {@code GET .../conclusions/stream}. Le contenu final reste
 * récupéré via le {@code GET .../conclusions} existant : {@code partialContent} sert
 * uniquement à l'aperçu live.</p>
 *
 * @param tokens         nombre de tokens de sortie reçus (approx. via la longueur du
 *                       partiel, ou compteur réel du SDK quand disponible)
 * @param maxTokens      budget de tokens de sortie ({@code MAX_TOKENS = 8000})
 * @param percent        progression {@code 0..100} = {@code min(100, round(tokens/maxTokens*100))}
 * @param currentSection dernier titre markdown {@code ##}/{@code ###} détecté, ou {@code null}
 * @param sectionIndex   index 1-based du titre détecté, ou {@code null}
 * @param partialContent markdown partiel accumulé jusqu'ici
 */
public record ConclusionStreamProgress(
        int tokens,
        int maxTokens,
        int percent,
        String currentSection,
        Integer sectionIndex,
        String partialContent) {
}
