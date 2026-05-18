package fr.ailegalcase.casefile.conclusion;

/**
 * F-98 — Fournit le prompt système d'une cellule de la matrice F-98.
 *
 * <p>Une cellule = un {@code @Component} implémentant cette interface. Chaque
 * implémentation déclare la combinaison procédurale qu'elle couvre via
 * {@link #combination()} et le prompt système de rédaction via
 * {@link #systemPrompt()}. Les implémentations sont agrégées au démarrage par
 * {@link ConclusionPromptRegistry}.</p>
 */
public interface ConclusionPromptProvider {

    /** @return la combinaison procédurale couverte par cette cellule. */
    CombinationKey combination();

    /**
     * @return le prompt système de rédaction de la cellule (instructions stables,
     *         cachables). La consigne de style F-98-47 est appliquée par-dessus
     *         par {@link CaseConclusionPromptBuilder}.
     */
    String systemPrompt();
}
