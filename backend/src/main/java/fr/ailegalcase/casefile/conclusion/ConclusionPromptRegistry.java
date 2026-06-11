package fr.ailegalcase.casefile.conclusion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * F-98 — Registre des cellules de la matrice de génération F-98.
 *
 * <p>Agrège tous les {@link ConclusionPromptProvider} injectés par Spring et les
 * indexe par {@link CombinationKey}. Permet à {@link CaseConclusionCommandService}
 * (garde {@code COMBINATION_NOT_SUPPORTED}) et à {@link CaseConclusionPromptBuilder}
 * (assemblage du prompt) de consulter les cellules disponibles sans connaître les
 * implémentations.</p>
 *
 * <p><strong>Fail-fast</strong> : si deux providers déclarent la même
 * {@link CombinationKey}, une {@link IllegalStateException} est levée au démarrage
 * — une cellule de matrice ne peut avoir qu'un prompt.</p>
 */
@Component
public class ConclusionPromptRegistry {

    private static final Logger log = LoggerFactory.getLogger(ConclusionPromptRegistry.class);

    private final Map<CombinationKey, ConclusionPromptProvider> providersByKey;

    /**
     * @param providers les cellules déclarées dans le contexte Spring
     * @throws IllegalStateException si deux providers déclarent la même combinaison
     */
    public ConclusionPromptRegistry(List<ConclusionPromptProvider> providers) {
        Map<CombinationKey, ConclusionPromptProvider> index = new HashMap<>();
        for (ConclusionPromptProvider provider : providers) {
            CombinationKey key = provider.combination();
            ConclusionPromptProvider previous = index.put(key, provider);
            if (previous != null) {
                throw new IllegalStateException("Deux providers de prompt déclarent la même "
                        + "combinaison F-98 " + key + " : "
                        + previous.getClass().getName() + " et " + provider.getClass().getName());
            }
        }
        this.providersByKey = Map.copyOf(index);
        log.info("Registre des prompts de conclusions F-98 initialisé — {} cellule(s)", index.size());
    }

    /** @return {@code true} si une cellule couvre la combinaison {@code key}. */
    public boolean supports(CombinationKey key) {
        return providersByKey.containsKey(key);
    }

    /**
     * @return le prompt système de base de la cellule couvrant {@code key},
     *         ou {@link Optional#empty()} si aucune cellule ne la couvre
     */
    public Optional<String> systemPrompt(CombinationKey key) {
        return Optional.ofNullable(providersByKey.get(key))
                .map(ConclusionPromptProvider::systemPrompt);
    }

    /** @return le nombre de cellules enregistrées (utile aux tests). */
    public int size() {
        return providersByKey.size();
    }

    /**
     * @return l'ensemble immuable des combinaisons couvertes par une cellule.
     *         Utilisé par le test d'intégrité {@code ConclusionCombinationCoverageIT}
     *         qui croise les combinaisons sélectionnables (catalogue F-243) avec les
     *         cellules génératrices (registre F-98) pour empêcher tout trou de
     *         couverture muet (cf. signal terrain BCO, SF-98-62).
     */
    public java.util.Set<CombinationKey> registeredCombinations() {
        return providersByKey.keySet();
    }
}
