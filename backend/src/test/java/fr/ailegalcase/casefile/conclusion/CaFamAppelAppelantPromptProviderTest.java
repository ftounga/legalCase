package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-36 — tests de la cellule CA_FAM / APPEL / APPELANT : combinaison
 * déclarée et prompt système (appel familial devant la cour d'appel, structure
 * art. 954 CPC, critique du jugement du JAF, dispositif « INFIRMER »).
 */
class CaFamAppelAppelantPromptProviderTest {

    private final CaFamAppelAppelantPromptProvider provider = new CaFamAppelAppelantPromptProvider();

    @Test
    void combination_isFamilyAppealFranceClaimantSide() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CA_FAM");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("APPELANT");
    }

    @Test
    void systemPrompt_targetsFamilyAppealStructure() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("chambre de la famille");
        assertThat(prompt).contains("cour d'appel");
        assertThat(prompt).contains("article 954");
        assertThat(prompt).contains("juge aux affaires familiales (JAF)");
        assertThat(prompt).contains("INFIRMER");
    }
}
