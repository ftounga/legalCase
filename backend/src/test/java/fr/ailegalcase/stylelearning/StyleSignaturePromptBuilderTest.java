package fr.ailegalcase.stylelearning;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-46 — tests unitaires du prompt d'extraction de style.
 *
 * <p>Vérifie l'invariant 1 du cadrage (minimisation RGPD) : le prompt système
 * interdit explicitement la reprise de tout fait / nom / date / montant / donnée
 * de dossier.</p>
 */
class StyleSignaturePromptBuilderTest {

    private final StyleSignaturePromptBuilder builder = new StyleSignaturePromptBuilder();

    @Test
    void systemPrompt_forbidsReuseOfCaseData() {
        String prompt = builder.buildSystemPrompt().toLowerCase();

        assertThat(prompt).contains("style");
        // Interdiction explicite de reprendre les données du dossier.
        assertThat(prompt).contains("aucun fait");
        assertThat(prompt).contains("aucun nom");
        assertThat(prompt).contains("aucune date");
        assertThat(prompt).contains("aucun montant");
        assertThat(prompt).contains("aucune donnée");
    }

    @Test
    void systemPrompt_describesStyleDimensions() {
        String prompt = builder.buildSystemPrompt().toLowerCase();

        assertThat(prompt).contains("structure");
        assertThat(prompt).contains("transition");
        assertThat(prompt).contains("registre");
        assertThat(prompt).contains("ton");
    }

    @Test
    void userMessage_carriesExtractedText() {
        String userMessage = builder.buildUserMessage("Texte de conclusion extrait.");

        assertThat(userMessage).contains("Texte de conclusion extrait.");
    }

    @Test
    void userMessage_nullTextIsTolerated() {
        assertThat(builder.buildUserMessage(null)).isNotNull();
    }
}
