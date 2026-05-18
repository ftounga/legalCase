package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-41 — tests de la cellule TF / FOND / DEMANDEUR (Belgique) :
 * combinaison déclarée et prompt système ancré dans la procédure familiale belge.
 */
class TfFondDemandeurPromptProviderTest {

    private final TfFondDemandeurPromptProvider provider = new TfFondDemandeurPromptProvider();

    @Test
    void combination_isFamilyTribunalBelgiumFondClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TF");
        assertThat(key.stage()).isEqualTo("FOND");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsBelgianFamilyTribunalAndClaimantRole() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur devant le tribunal de la famille");
        assertThat(prompt).contains("tribunal de la famille");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("Code civil belge");
        assertThat(prompt).contains("loi du 30 juillet 2013");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS, plaise au Tribunal de la famille");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("juge aux affaires familiales");
        assertThat(prompt).doesNotContain("tribunal judiciaire");
    }
}
