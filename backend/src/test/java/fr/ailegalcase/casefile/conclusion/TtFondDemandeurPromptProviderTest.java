package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-11 — tests de la cellule TT / FOND / DEMANDEUR (Belgique) :
 * combinaison déclarée et prompt système ancré dans la procédure belge.
 */
class TtFondDemandeurPromptProviderTest {

    private final TtFondDemandeurPromptProvider provider = new TtFondDemandeurPromptProvider();

    @Test
    void combination_isWorkTribunalBelgiumFondClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TT");
        assertThat(key.stage()).isEqualTo("FOND");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsBelgianWorkTribunalAndClaimantRole() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur (travailleur)");
        assertThat(prompt).contains("tribunal du travail");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("loi du 3 juillet 1978");
        assertThat(prompt).contains("CCT n° 109");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS, plaise au Tribunal du travail");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("Conseil de prud'hommes");
        assertThat(prompt).doesNotContain("Code du travail");
    }
}
