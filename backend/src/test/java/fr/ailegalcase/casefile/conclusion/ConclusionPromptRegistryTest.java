package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * F-98 / SF-98-02 — tests du registre des cellules de la matrice F-98.
 *
 * <p>Couvre {@code supports} / {@code systemPrompt} (combinaison connue / inconnue), la
 * détection de doublon de clé (fail-fast), et un contrôle data-driven sur toutes les
 * cellules du contexte Spring : prompt non vide et codes de combinaison résolus par
 * {@link ProcedureStageCatalog}. Ce dernier test couvrira automatiquement les cellules
 * ajoutées par les SF-98-03→10.</p>
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=test-google-id",
        "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
        "anthropic.api-key=test-key"
})
class ConclusionPromptRegistryTest {

    /** Toutes les cellules déclarées dans le contexte Spring (demandeur, défendeur, SF-98-03→10…). */
    @Autowired
    private List<ConclusionPromptProvider> allProviders;

    @Autowired
    private ConclusionPromptRegistry registry;

    private static final CombinationKey DEMANDEUR_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.FRANCE,
            "CPH", "FOND", "DEMANDEUR");

    private static final CombinationKey DEFENDEUR_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.FRANCE,
            "CPH", "FOND", "DEFENDEUR");

    /**
     * Combinaison <strong>volontairement jamais enregistrée</strong> par la matrice F-98 :
     * le bureau de conciliation et d'orientation (BCO) est un stade de conciliation, pas un
     * stade de rédaction de conclusions — aucune cellule F-98 ne le couvre, à 53/53 comme
     * aujourd'hui. À utiliser comme combinaison « inconnue » stable des tests du registre
     * (ne jamais y mettre une combinaison réelle de la matrice : elle finirait couverte).
     */
    private static final CombinationKey UNKNOWN_KEY = new CombinationKey(
            ProcedureStageCatalog.DROIT_DU_TRAVAIL, ProcedureStageCatalog.FRANCE,
            "CPH", "BCO", "DEMANDEUR");

    @Test
    void supports_knownCombination_isTrue() {
        assertThat(registry.supports(DEMANDEUR_KEY)).isTrue();
        assertThat(registry.supports(DEFENDEUR_KEY)).isTrue();
    }

    @Test
    void supports_unknownCombination_isFalse() {
        assertThat(registry.supports(UNKNOWN_KEY)).isFalse();
    }

    @Test
    void systemPrompt_knownCombination_returnsNonBlankPrompt() {
        Optional<String> prompt = registry.systemPrompt(DEMANDEUR_KEY);
        assertThat(prompt).isPresent();
        assertThat(prompt.get()).isNotBlank();
    }

    @Test
    void systemPrompt_unknownCombination_isEmpty() {
        assertThat(registry.systemPrompt(UNKNOWN_KEY)).isEmpty();
    }

    @Test
    void size_matchesNumberOfProviders() {
        assertThat(registry.size()).isEqualTo(allProviders.size());
        assertThat(registry.size()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void constructor_duplicateCombinationKey_throwsIllegalState() {
        // Deux providers déclarant la même CombinationKey — interdit (une cellule = un prompt).
        ConclusionPromptProvider first = new CphFondDemandeurPromptProvider();
        ConclusionPromptProvider duplicate = new CphFondDemandeurPromptProvider();

        assertThatThrownBy(() -> new ConclusionPromptRegistry(List.of(first, duplicate)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("même combinaison");
    }

    // ── Data-driven — couvre automatiquement les cellules SF-98-03→10 ───────────

    @Test
    void everyRegisteredProvider_hasNonBlankPromptAndResolvableCodes() {
        assertThat(allProviders).isNotEmpty();
        for (ConclusionPromptProvider provider : allProviders) {
            CombinationKey key = provider.combination();

            assertThat(provider.systemPrompt())
                    .as("prompt système de %s", provider.getClass().getSimpleName())
                    .isNotBlank();

            assertThat(ProcedureStageCatalog.jurisdictionLabel(
                    key.domain(), key.country(), key.jurisdiction()))
                    .as("juridiction %s de %s", key.jurisdiction(),
                            provider.getClass().getSimpleName())
                    .isNotBlank();
            assertThat(ProcedureStageCatalog.stageLabel(
                    key.domain(), key.country(), key.stage()))
                    .as("stade %s de %s", key.stage(), provider.getClass().getSimpleName())
                    .isNotBlank();
            assertThat(ProcedureStageCatalog.positionLabel(
                    key.domain(), key.country(), key.position()))
                    .as("position %s de %s", key.position(),
                            provider.getClass().getSimpleName())
                    .isNotBlank();
        }
    }
}
