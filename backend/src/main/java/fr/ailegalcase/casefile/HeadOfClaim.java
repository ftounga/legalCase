package fr.ailegalcase.casefile;

/**
 * F-262 SF-262-01 — Chef de demande du référentiel (catalogue code-based).
 *
 * <p>Un chef de demande est une prétention que l'avocat peut inscrire au
 * dispositif de ses conclusions. Le filet de complétude (F-262) en détecte
 * l'<em>applicabilité</em> au dossier et signale s'il est <em>couvert</em>
 * (chef outillé calculé) ou seulement rappelé (chef transverse).</p>
 *
 * @param code      identifiant stable du chef (ex. {@code art-700-cpc}).
 * @param label     libellé court lisible par l'avocat.
 * @param fondement fondement juridique (texte / article).
 * @param category  {@link HeadCategory} — outillé (mappé F-DT) ou transverse.
 * @param toolId    identifiant de l'outil décisionnel mappé (TOOL_REGISTRY),
 *                  ou {@code null} pour un chef transverse non outillé.
 */
public record HeadOfClaim(
        String code,
        String label,
        String fondement,
        HeadCategory category,
        String toolId
) {

    /** Catégorie d'un chef de demande. */
    public enum HeadCategory {
        /** Chef indemnitaire mappé à un outil décisionnel (applicabilité = visibilité de l'outil). */
        INDEMNITAIRE_OUTILLE,
        /** Chef transverse non outillé (art. 700, dépens, intérêts, capitalisation, exécution provisoire). */
        TRANSVERSE
    }
}
