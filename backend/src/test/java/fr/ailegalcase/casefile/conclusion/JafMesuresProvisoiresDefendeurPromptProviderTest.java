package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-33 — tests de la cellule JAF / MESURES_PROVISOIRES / DEFENDEUR :
 * combinaison déclarée et prompt système (rôle défendeur, mesures provisoires en
 * défense, ancrage code civil art. 254-255).
 */
class JafMesuresProvisoiresDefendeurPromptProviderTest {

    private final JafMesuresProvisoiresDefendeurPromptProvider provider =
            new JafMesuresProvisoiresDefendeurPromptProvider();

    @Test
    void combination_isFamilyLawFranceJafProvisionalMeasuresDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("JAF");
        assertThat(key.stage()).isEqualTo("MESURES_PROVISOIRES");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsDefendantRoleJafAndProvisionalMeasures() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur");
        assertThat(prompt).contains("juge aux affaires familiales");
        assertThat(prompt).contains("mesures provisoires");
        assertThat(prompt).contains("CONCLUSIONS EN DÉFENSE");
        assertThat(prompt).contains("articles 254 et 255 du code civil");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
