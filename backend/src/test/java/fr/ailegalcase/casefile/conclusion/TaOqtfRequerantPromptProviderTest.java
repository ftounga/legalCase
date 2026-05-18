package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-18 — tests de la cellule TA / RECOURS_OQTF / REQUERANT :
 * combinaison déclarée et prompt système (requête en annulation d'une OQTF,
 * contentieux administratif, moyens de légalité externe / interne).
 */
class TaOqtfRequerantPromptProviderTest {

    private final TaOqtfRequerantPromptProvider provider = new TaOqtfRequerantPromptProvider();

    @Test
    void combination_isImmigrationFranceTaOqtfRequerant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("TA");
        assertThat(key.stage()).isEqualTo("RECOURS_OQTF");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsAdministrativeOqtfAnnulmentRequest() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("tribunal administratif");
        assertThat(prompt).contains("REQUÊTE EN ANNULATION");
        assertThat(prompt).contains("OQTF");
        assertThat(prompt).contains("CESEDA");
        assertThat(prompt).contains("LÉGALITÉ EXTERNE");
        assertThat(prompt).contains("LÉGALITÉ INTERNE");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("Pièce n° X");
    }
}
