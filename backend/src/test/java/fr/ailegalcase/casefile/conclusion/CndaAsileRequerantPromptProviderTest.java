package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-23 — tests de la cellule CNDA / RECOURS_ASILE / REQUERANT :
 * combinaison déclarée et prompt système (recours asile, plein contentieux,
 * statut de réfugié, protection subsidiaire, convention de Genève).
 */
class CndaAsileRequerantPromptProviderTest {

    private final CndaAsileRequerantPromptProvider provider = new CndaAsileRequerantPromptProvider();

    @Test
    void combination_isImmigrationFranceCndaAsileClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CNDA");
        assertThat(key.stage()).isEqualTo("RECOURS_ASILE");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsAsylumAppealMarkers() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("CNDA");
        assertThat(prompt).contains("PLEIN CONTENTIEUX");
        assertThat(prompt).contains("STATUT DE RÉFUGIÉ");
        assertThat(prompt).contains("PROTECTION SUBSIDIAIRE");
        assertThat(prompt).contains("convention de Genève du 28 juillet 1951");
    }
}
