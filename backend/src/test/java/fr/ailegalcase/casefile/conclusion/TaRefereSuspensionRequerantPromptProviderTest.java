package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-20 — tests de la cellule TA / REFERE_SUSPENSION / REQUERANT :
 * combinaison déclarée et prompt système (référé-suspension, art. L.521-1 CJA,
 * urgence, doute sérieux, accessoire au recours au fond).
 */
class TaRefereSuspensionRequerantPromptProviderTest {

    private final TaRefereSuspensionRequerantPromptProvider provider =
            new TaRefereSuspensionRequerantPromptProvider();

    @Test
    void combination_isImmigrationFranceTaRefereSuspensionRequerant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("TA");
        assertThat(key.stage()).isEqualTo("REFERE_SUSPENSION");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsRefereSuspensionStandardAndStructure() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("référé-suspension");
        assertThat(prompt).contains("L.521-1");
        assertThat(prompt).contains("urgence");
        assertThat(prompt).contains("doute sérieux");
        assertThat(prompt).contains("recours en annulation au fond");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("Pièce n° X");
    }
}
